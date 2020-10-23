package one.edee.darwin.locker;
/**
 * Description
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 * @version $Id$
 */

import one.edee.darwin.AbstractDbAutoupdateTest;
import one.edee.darwin.Locker;
import one.edee.darwin.exception.ProcessIsLockedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class LockerTest extends AbstractDbAutoupdateTest {
	@Autowired private Locker locker;
	private Calendar now, nowPlusHour, nowMinusHour, nowPlusDay;

	@BeforeEach
    public void initTest() throws Exception {
		now = new GregorianCalendar();
		nowPlusHour = new GregorianCalendar(
				now.get(Calendar.YEAR),
				now.get(Calendar.MONTH),
				now.get(Calendar.DAY_OF_MONTH),
				now.get(Calendar.HOUR_OF_DAY) + 1,
				now.get(Calendar.MINUTE),
				now.get(Calendar.SECOND)
		);
		nowMinusHour = new GregorianCalendar(
				now.get(Calendar.YEAR),
				now.get(Calendar.MONTH),
				now.get(Calendar.DAY_OF_MONTH),
				now.get(Calendar.HOUR_OF_DAY) - 1,
				now.get(Calendar.MINUTE),
				now.get(Calendar.SECOND)
		);
		nowPlusDay = new GregorianCalendar(
				now.get(Calendar.YEAR),
				now.get(Calendar.MONTH),
				now.get(Calendar.DAY_OF_MONTH) + 1,
				now.get(Calendar.HOUR_OF_DAY),
				now.get(Calendar.MINUTE),
				now.get(Calendar.SECOND)
		);
	}

	@Test
	public void testLeaseProcess() throws Exception {
		String processName = "processTest1";
		String unlockKey = locker.leaseProcess(processName, nowPlusHour.getTime());
		locker.releaseProcess(processName, unlockKey);
	}

	@Test
	public void testLockCheck() throws Exception {
		String processName = "processTest3";
		assertTrue(locker.canLease(processName));
		String unlockKey = locker.leaseProcess(processName, nowPlusHour.getTime());
		assertFalse(locker.canLease(processName));
		locker.releaseProcess(processName, unlockKey);
		assertTrue(locker.canLease(processName));
	}

	@Test
	public void testReleaseProcess() throws Exception {
		String processName = "processTest4";
		String unlockKey = locker.leaseProcess(processName, nowPlusHour.getTime());
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
		String unlockKey = locker.leaseProcess(processName, nowPlusHour.getTime());
		try {
			locker.leaseProcess(processName, nowPlusDay.getTime());
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
		try{
			unlockKey = locker.leaseProcess(processName, new Date(System.currentTimeMillis() + 1000));
			anotherUnlockKey = locker.leaseProcess(processName, nowPlusDay.getTime(), 2500);
			assertTrue(!unlockKey.equals(anotherUnlockKey));
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
		String unlockKey = locker.leaseProcess(processName, nowMinusHour.getTime());
		try {
			unlockKey = locker.leaseProcess(processName, nowPlusDay.getTime());
		}
		finally {
			locker.releaseProcess(processName, unlockKey);
		}
	}

	@Test
	public void testRenewLease() throws Exception {
		String processName = "processTest8";
		String unlockKey = locker.leaseProcess(processName, nowPlusHour.getTime());
		try {
			locker.renewLease(processName, unlockKey, nowPlusDay.getTime());
		}
		finally {
			locker.releaseProcess(processName, unlockKey);
		}
	}

	@Test
	public void testRenewLeaseWithRestorer() {
		final AtomicBoolean finished = new AtomicBoolean();
		final AtomicInteger counter = new AtomicInteger();

		String unlockKey = "";
		LockRestorer lockRestorer = new LockRestorer() {
			@Override
			public boolean isFinished() {
				counter.incrementAndGet();
				return finished.get();
			}
		};

		try {
			unlockKey = locker.leaseProcess("testProcess", new Date(System.currentTimeMillis() + 1000), lockRestorer);
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