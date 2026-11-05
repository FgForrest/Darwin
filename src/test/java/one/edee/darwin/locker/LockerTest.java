package one.edee.darwin.locker;
/**
 * Description
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 * @version $Id$
 */

import one.edee.darwin.AbstractDarwinTest;
import one.edee.darwin.exception.ProcessIsLockedException;
import one.edee.darwin.spring.DarwinConfiguration;
import one.edee.darwin.utils.spring.InstanceIdConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@ContextConfiguration(
		classes = {
				DarwinConfiguration.class,
				InstanceIdConfiguration.class
		}
)
public abstract class LockerTest extends AbstractDarwinTest {
	@Autowired private Locker locker;
	@Autowired private InstanceIdProvider instanceIdProvider;
	private LocalDateTime now, nowPlusHour, nowMinusHour, nowPlusDay;

	@BeforeEach
    public void initTest() throws Exception {
		now = LocalDateTime.now();
		nowPlusHour = now.plusHours(1);
		nowMinusHour = now.minusHours(1);
		nowPlusDay = now.plusDays(1);
		locker.setRetryTimes(1);
	}

	@AfterEach
	void tearDown() {
		// set back to default
		locker.setRetryTimes(20);
	}

	@Test
	public void testLeaseProcess() throws Exception {
		String processName = "processTest1";
		String unlockKey = locker.leaseProcess(processName, nowPlusHour);
		locker.releaseProcess(processName, unlockKey);
	}

	@Test
	public void testLockCheck() throws Exception {
		String processName = "processTest3";
		assertTrue(locker.canLease(processName));
		String unlockKey = locker.leaseProcess(processName, nowPlusHour);
		assertFalse(locker.canLease(processName));
		locker.releaseProcess(processName, unlockKey);
		assertTrue(locker.canLease(processName));
	}

	@Test
	public void testLockRelease() throws Exception {
		InstanceIdConfiguration.TestInstanceIdProvider testInstanceIdProvider = (InstanceIdConfiguration.TestInstanceIdProvider) instanceIdProvider;

		locker.leaseProcess("process1", nowPlusHour);
		locker.leaseProcess("process2", nowPlusHour);
		locker.leaseProcess("process3", nowPlusHour);
		final String oldId = testInstanceIdProvider.getInstanceId();
		testInstanceIdProvider.setNodeId("newId");
		locker.leaseProcess("processNew2", nowPlusHour);
		locker.leaseProcess("processNew3", nowPlusHour);
		assertEquals(2, locker.releaseProcessesForInstance());
		assertEquals(0, locker.releaseProcessesForInstance());

		testInstanceIdProvider.setNodeId(oldId);
		assertEquals(3, locker.releaseProcessesForInstance());
		assertEquals(0, locker.releaseProcessesForInstance());
	}

	@Test
	public void testReleaseProcess() throws Exception {
		String processName = "processTest4";
		String unlockKey = locker.leaseProcess(processName, nowPlusHour);
		try {
			locker.releaseProcess(processName, "badUnlockKey");
			fail("Exception expected - bad unlock key");
		}
		catch(IllegalStateException ex) {
			//ok
		}
		finally {
			locker.releaseProcess(processName, unlockKey);
		}
	}

	@Test
	public void testLeaseProcessTwice() throws Exception {
		String processName = "processTest5";
		String unlockKey = locker.leaseProcess(processName, nowPlusHour);
		try {
			locker.leaseProcess(processName, nowPlusDay);
			fail("Exception expected - lock should not be available!");
		}
		catch(ProcessIsLockedException ex) {
			//ok
		}
		finally {
			locker.releaseProcess(processName, unlockKey);
		}
	}

	@Test
	public void testLeaseProcessWithTimeout() throws Exception {
		String processName = "processTest6";
		String unlockKey = null;
		String anotherUnlockKey = null;
		locker.setRetryTimes(2);
		try{
			unlockKey = locker.leaseProcess(processName, LocalDateTime.now().plusSeconds(1));
			anotherUnlockKey = locker.leaseProcess(processName, nowPlusDay, 2500);
			assertNotEquals(anotherUnlockKey, unlockKey);
		} catch(AssertionFailedError ex) {
			locker.releaseProcess(processName, unlockKey);
			throw ex;
		} finally {
			locker.releaseProcess(processName, anotherUnlockKey);
		}
	}

	@Test
	public void testLeaseProcessWithExpiredLock() throws Exception {
		String processName = "processTest7";
		String unlockKey = locker.leaseProcess(processName, nowMinusHour);
		try {
			unlockKey = locker.leaseProcess(processName, nowPlusDay);
		}
		finally {
			locker.releaseProcess(processName, unlockKey);
		}
	}

	@Test
	public void testRenewLease() throws Exception {
		String processName = "processTest8";
		String unlockKey = locker.leaseProcess(processName, nowPlusHour);
		try {
			locker.renewLease(processName, unlockKey, nowPlusDay);
		}
		finally {
			locker.releaseProcess(processName, unlockKey);
		}
	}

    @Test
    public void testRenewLeaseWithRestorer() throws Exception {
        final AtomicBoolean finished = new AtomicBoolean();
        final AtomicInteger counter = new AtomicInteger();
        LockRestorer restorer = () -> {
            counter.incrementAndGet();
            return finished.get();
        };
        // Renewal runs at 70 % of the lease; the remaining 30 % (1.8 s) must exceed the up to 1.5 s error of
        // second-precision database time (MySQL now() truncation plus DATETIME rounding).
        String key = locker.leaseProcess("testProcess", LocalDateTime.now().plusSeconds(6), restorer);
        try {
            String sql = "select leaseUntil from DARWIN_LOCK where processName = ?";
            LocalDateTime original = getJdbcTemplate().queryForObject(sql, LocalDateTime.class, "testProcess");
            // Every renewal must happen while the previous lease is still valid, otherwise the guarded
            // renewal is rejected and the lease silently expires; observe several consecutive cycles.
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(25);
            while (counter.get() < 3 && System.nanoTime() < deadline) {
                Thread.sleep(50);
                assertFalse(locker.canLease("testProcess"), "Lease expired after " + counter.get() + " restorer checks");
            }
            assertTrue(counter.get() >= 3, "The restorer must keep running for several cycles");
            Thread.sleep(500);
            assertFalse(locker.canLease("testProcess"), "Lease must stay valid after repeated renewals");
            LocalDateTime renewed = getJdbcTemplate().queryForObject(sql, LocalDateTime.class, "testProcess");
            assertTrue(renewed.isAfter(original), "The restorer must actually extend the database lease");
        } finally {
            finished.set(true);
            locker.releaseProcess("testProcess", key);
        }
    }

    @Test
    public void expiredLeaseCannotBeRenewedEvenBeforeTakeover() throws Exception {
        String process = "expired-renew-" + java.util.UUID.randomUUID();
        String key = locker.leaseProcess(process, nowMinusHour);
        try {
            assertThrows(ProcessIsLockedException.class, () -> locker.renewLease(process, key, nowPlusDay));
            assertTrue(locker.canLease(process));
        } finally { locker.releaseProcess(process, key); }
    }

    @Test
    public void wrongKeyCannotDeleteAnExpiredOwnersLock() throws Exception {
        String process = "wrong-expired-release-" + java.util.UUID.randomUUID();
        String key = locker.leaseProcess(process, nowMinusHour);
        assertThrows(IllegalStateException.class, () -> locker.releaseProcess(process, "foreign-key"));
        locker.releaseProcess(process, key);
    }

    @Test
    public void reacquisitionUsesNewOpaqueIdentityAndRejectsOldKey() throws Exception {
        String process = "aba-" + java.util.UUID.randomUUID();
        String previous = locker.leaseProcess(process, nowPlusHour);
        locker.releaseProcess(process, previous);
        String current = locker.leaseProcess(process, nowPlusHour);
        try {
            assertNotEquals(previous, current);
            assertThrows(ProcessIsLockedException.class, () -> locker.renewLease(process, previous, nowPlusDay));
            assertThrows(IllegalStateException.class, () -> locker.releaseProcess(process, previous));
            assertFalse(locker.canLease(process));
        } finally { locker.releaseProcess(process, current); }
    }

    @Test
    public void simultaneousExpiredTakeoverHasExactlyOneWinner() throws Exception {
        String process = "takeover-" + java.util.UUID.randomUUID();
        locker.leaseProcess(process, nowMinusHour);
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<String> attempt = () -> {
            start.await();
            try { return locker.leaseProcess(process, nowPlusHour); }
            catch (ProcessIsLockedException expected) { return null; }
        };
        String winner = null;
        try {
            java.util.concurrent.Future<String> a = pool.submit(attempt), b = pool.submit(attempt);
            start.countDown();
            String x = a.get(10, java.util.concurrent.TimeUnit.SECONDS), y = b.get(10, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue((x == null) != (y == null)); winner = x == null ? y : x;
        } finally { pool.shutdownNow(); if (winner != null) locker.releaseProcess(process, winner); }
    }

    @Test
    public void lostRenewDoesNotUseConfiguredRetryDelay() {
        locker.setRetryTimes(20); locker.setDefaultRetryWaitTime(3000);
        assertTimeout(java.time.Duration.ofSeconds(2), () ->
            assertThrows(ProcessIsLockedException.class, () -> locker.renewLease("missing-renew-fixture", "old-key", nowPlusHour)));
    }

}
