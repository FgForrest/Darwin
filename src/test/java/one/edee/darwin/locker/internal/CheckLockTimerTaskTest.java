package one.edee.darwin.locker.internal;

import one.edee.darwin.exception.ProcessIsLockedException;
import one.edee.darwin.locker.Locker;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Regression coverage for failures inside the scheduled lease renewal task. */
class CheckLockTimerTaskTest {

    @Test
    void transientRenewalFailureDoesNotStopFurtherRenewals() {
        AtomicInteger attempts = new AtomicInteger();
        Locker locker = new Locker() {
            @Override
            public void renewLeaseOnce(String processName, String unlockKey, LocalDateTime until) {
                if (attempts.incrementAndGet() == 1) {
                    throw new IllegalStateException("database temporarily unavailable");
                }
            }
        };
        locker.getProcessMap().put("process" + "key", () -> false);
        CheckLockTimerTask task = new CheckLockTimerTask(locker, "process", "key", 1000);

        // An exception escaping run() would make ScheduledExecutorService cancel all future executions.
        assertDoesNotThrow(task::run);
        assertDoesNotThrow(task::run);
        assertEquals(2, attempts.get());
        assertTrue(locker.getProcessMap().containsKey("processkey"));
    }

    @Test
    void lostOwnershipStopsRenewing() {
        AtomicInteger attempts = new AtomicInteger();
        Locker locker = new Locker() {
            @Override
            public void renewLeaseOnce(String processName, String unlockKey, LocalDateTime until) throws ProcessIsLockedException {
                attempts.incrementAndGet();
                throw new ProcessIsLockedException("lost");
            }
        };
        locker.getProcessMap().put("process" + "key", () -> false);
        CheckLockTimerTask task = new CheckLockTimerTask(locker, "process", "key", 1000);

        assertDoesNotThrow(task::run);
        assertFalse(locker.getProcessMap().containsKey("processkey"), "Lost lease cannot become valid again");
        assertDoesNotThrow(task::run);
        assertEquals(1, attempts.get());
    }

    @Test
    void finishedProcessCancelsItsScheduledExecution() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        try {
            CheckLockTimerTask task = new CheckLockTimerTask(new Locker(), "process", "key", 1000);
            ScheduledFuture<?> future = executor.scheduleAtFixedRate(task, 1, 1, TimeUnit.HOURS);
            task.setScheduledFuture(future);
            // the process is not registered in processMap any more (released or finished)
            task.run();
            assertTrue(future.isCancelled(), "Finished renewal must not stay scheduled forever");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void cancellationBeforeFutureIsKnownIsAppliedLater() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        try {
            CheckLockTimerTask task = new CheckLockTimerTask(new Locker(), "process", "key", 1000);
            task.run();
            ScheduledFuture<?> future = executor.scheduleAtFixedRate(task, 1, 1, TimeUnit.HOURS);
            task.setScheduledFuture(future);
            assertTrue(future.isCancelled());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void renewalLimitKeepsOriginalProtectionWindow() {
        AtomicInteger attempts = new AtomicInteger();
        Locker locker = new Locker() {
            @Override
            public void renewLeaseOnce(String processName, String unlockKey, LocalDateTime until) {
                attempts.incrementAndGet();
            }
        };
        locker.getProcessMap().put("process" + "key", () -> false);
        CheckLockTimerTask task = new CheckLockTimerTask(locker, "process", "key", 1000);
        for (int i = 0; i < 40; i++) {
            task.run();
        }
        // checks every third of the lease: 29 checks end at 9.67 lease periods and keep the lock for 10.67 periods,
        // the same window as the former 10 renewals at 0.7, 1.7, ... 9.7 lease periods
        assertEquals(29, attempts.get());
    }
}
