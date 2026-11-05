package one.edee.darwin.locker;

import one.edee.darwin.exception.ProcessIsLockedException;
import one.edee.darwin.model.LockState;
import one.edee.darwin.storage.LockStorage;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Regression coverage for clock normalization, owner identities and fail-fast renewal. */
class LockerLeaseContractTest {
    @Test
    void normalizesJvmDeadlineExactlyOnceInBothClockDirections() throws Exception {
        for (int offset : new int[]{-120, 120}) {
            try (GenericApplicationContext context = context("test-instance")) {
                RecordingStorage storage = new RecordingStorage(offset);
                Locker locker = locker(storage, context);
                LocalDateTime before = LocalDateTime.now();
                String key = locker.leaseProcess("clock", before.plusSeconds(60));
                assertEquals(key, UUID.fromString(key).toString());
                assertFalse(storage.deadline.isBefore(before.plusSeconds(offset + 59)));
                assertFalse(storage.deadline.isAfter(LocalDateTime.now().plusSeconds(offset + 61)));
            }
        }
    }

    @Test
    void lostLeaseIsAttemptedOnceDespiteConfiguredRetries() {
        try (GenericApplicationContext context = context("test-instance")) {
            RecordingStorage storage = new RecordingStorage(0);
            Locker locker = locker(storage, context);
            locker.setRetryTimes(20);
            locker.setDefaultRetryWaitTime(3000);
            assertTimeout(Duration.ofSeconds(1), () -> assertThrows(ProcessIsLockedException.class,
                () -> locker.renewLease("lost", "old", LocalDateTime.now().plusMinutes(1))));
            assertEquals(1, storage.renewals);
        }
    }

    @Test
    void scheduledSingleRenewalDoesNotRetryOrWrapStorageFailure() {
        try (GenericApplicationContext context = context("test-instance")) {
            RecordingStorage storage = new RecordingStorage(0);
            storage.failure = new IllegalStateException("database unavailable");
            Locker locker = locker(storage, context);
            locker.setRetryTimes(20);
            locker.setDefaultRetryWaitTime(3000);
            assertTimeout(Duration.ofSeconds(1), () -> assertSame(storage.failure,
                assertThrows(IllegalStateException.class,
                    () -> locker.renewLeaseOnce("lost", "old", LocalDateTime.now().plusMinutes(1)))));
            assertEquals(1, storage.renewals);
        }
    }

    @Test
    void lostLeaseIsNotRetriedBySingleRenewalEither() {
        try (GenericApplicationContext context = context("test-instance")) {
            RecordingStorage storage = new RecordingStorage(0);
            Locker locker = locker(storage, context);
            locker.setRetryTimes(20);
            locker.setDefaultRetryWaitTime(3000);
            assertTimeout(Duration.ofSeconds(1), () -> assertThrows(ProcessIsLockedException.class,
                () -> locker.renewLeaseOnce("lost", "old", LocalDateTime.now().plusMinutes(1))));
            assertEquals(1, storage.renewals);
        }
    }

    @Test
    void disabledLockerFailsBeforeRenewalRetries() {
        try (GenericApplicationContext context = context("test-instance")) {
            RecordingStorage storage = new RecordingStorage(0);
            Locker locker = locker(storage, context);
            locker.afterPropertiesSet(); // no data source: skipIfDataSourceNotPresent switches it off
            locker.setRetryTimes(3);
            locker.setDefaultRetryWaitTime(600);
            assertTimeout(Duration.ofSeconds(1), () -> assertThrows(IllegalStateException.class,
                () -> locker.renewLease("disabled", "key", LocalDateTime.now().plusMinutes(1))));
            assertEquals(0, storage.renewals);
        }
    }

    @Test
    void publicRenewalStillRetriesTransientStorageFailure() throws Exception {
        try (GenericApplicationContext context = context("test-instance")) {
            RecordingStorage storage = new RecordingStorage(0);
            storage.failure = new IllegalStateException("connection reset");
            storage.failuresLeft = 1;
            storage.renewResult = LockState.LEASED;
            Locker locker = locker(storage, context);
            locker.setRetryTimes(3);
            locker.setDefaultRetryWaitTime(10);
            locker.renewLease("page-loop", "key", LocalDateTime.now().plusMinutes(1));
            assertEquals(2, storage.renewals);
        }
    }

    @Test
    void oversizedInstanceIdentityIsRejectedBeforeInsert() {
        String oversized = String.join("", java.util.Collections.nCopies(255, "x"));
        try (GenericApplicationContext context = context(oversized)) {
            RecordingStorage storage = new RecordingStorage(0);
            Locker locker = locker(storage, context);
            assertThrows(IllegalArgumentException.class,
                () -> locker.leaseProcess("too-long", LocalDateTime.now().plusMinutes(1)));
            assertEquals(0, storage.creations);
        }
    }

    @Test
    void oversizedInstanceIdentityIsNotRetriedWhileWaitingForLock() {
        String oversized = String.join("", java.util.Collections.nCopies(255, "x"));
        try (GenericApplicationContext context = context(oversized)) {
            RecordingStorage storage = new RecordingStorage(0);
            Locker locker = locker(storage, context);
            locker.setRetryTimes(20);
            // a configuration error is deterministic - waiting 20 x 3 s for it is pointless
            assertTimeout(Duration.ofSeconds(1), () -> assertThrows(IllegalArgumentException.class,
                () -> locker.leaseProcess("too-long", LocalDateTime.now().plusMinutes(1), 3000)));
            assertEquals(0, storage.creations);
        }
    }

    @Test
    void restorerToleratesOneMissedRenewalBeforeLeaseExpires() throws Exception {
        long[] schedule = new long[2];
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1) {
            @Override
            public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
                schedule[0] = unit.toMillis(initialDelay);
                schedule[1] = unit.toMillis(period);
                return super.scheduleAtFixedRate(command, 1, 1, TimeUnit.HOURS);
            }
        };
        try (GenericApplicationContext context = context("test-instance")) {
            context.getBeanFactory().registerSingleton("scheduler", executor);
            Locker locker = locker(new RecordingStorage(0), context);
            long lease = 60_000;
            locker.leaseProcess("scheduled", LocalDateTime.now().plusNanos(lease * 1_000_000), () -> false);
            // a renewal failing transiently must be retried by the next check while the last lease is still valid
            assertTrue(schedule[0] > 0 && schedule[0] <= lease / 2, "first renewal at " + schedule[0]);
            assertTrue(schedule[1] > 0 && 2 * schedule[1] < lease - 15_000, "renewal period " + schedule[1]);
        } finally {
            executor.shutdownNow();
        }
    }

    private static GenericApplicationContext context(String instance) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.getBeanFactory().registerSingleton("instanceIdProvider", (InstanceIdProvider) () -> instance);
        context.refresh();
        return context;
    }

    private static Locker locker(RecordingStorage storage, GenericApplicationContext context) {
        Locker locker = new Locker();
        locker.setApplicationContext(context);
        locker.setLockStorage(storage);
        return locker;
    }

    private static class RecordingStorage implements LockStorage {
        private final int offset;
        private LocalDateTime deadline;
        private int renewals;
        private int creations;
        private RuntimeException failure;
        private int failuresLeft = Integer.MAX_VALUE;
        private LockState renewResult = LockState.AVAILABLE;

        RecordingStorage(int offset) { this.offset = offset; }
        @Override public LocalDateTime getCurrentDatabaseTime() { return LocalDateTime.now().plusSeconds(offset); }
        @Override public int releaseProcessesForInstance(String instance) { return 0; }
        @Override public LockState getProcessLock(String name, LocalDateTime now) { return LockState.AVAILABLE; }
        @Override public LockState createLock(String name, LocalDateTime until, String key) {
            creations++;
            deadline = until;
            return LockState.LEASED;
        }
        @Override public LockState releaseProcess(String name, String key) { return LockState.AVAILABLE; }
        @Override public LockState renewLease(String name, String key, LocalDateTime until) {
            renewals++;
            if (failure != null && failuresLeft > 0) {
                failuresLeft--;
                throw failure;
            }
            return renewResult;
        }
    }
}
