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
	public void testRenewLeaseWithRestorer() throws ProcessIsLockedException {
		final AtomicBoolean finished = new AtomicBoolean();
		final AtomicInteger counter = new AtomicInteger();

		String unlockKey = "";
		LockRestorer lockRestorer = () -> {
			counter.incrementAndGet();
			return finished.get();
		};

		try {
			unlockKey = locker.leaseProcess("testProcess", LocalDateTime.now().plusSeconds(1), lockRestorer);
		} catch (ProcessIsLockedException e) {
			System.err.println(e);
		}
		try {
			// Set process time
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}
		finished.set(true);
		locker.releaseProcess("testProcess", unlockKey);
		try {
			// Set after process time
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		assertEquals(2, counter.get());
	}

}
