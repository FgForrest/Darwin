package one.edee.darwin.locker;

import one.edee.darwin.exception.ProcessIsLockedException;
import one.edee.darwin.model.LockState;
import one.edee.darwin.storage.LockStorage;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

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
    void storageFailureIsNotRetriedOrWrapped() {
        try (GenericApplicationContext context = context("test-instance")) {
            RecordingStorage storage = new RecordingStorage(0);
            storage.failure = new IllegalStateException("database unavailable");
            Locker locker = locker(storage, context);
            assertTimeout(Duration.ofSeconds(1), () -> assertSame(storage.failure,
                assertThrows(IllegalStateException.class,
                    () -> locker.renewLease("lost", "old", LocalDateTime.now().plusMinutes(1)))));
            assertEquals(1, storage.renewals);
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
            if (failure != null) throw failure;
            return LockState.AVAILABLE;
        }
    }
}
