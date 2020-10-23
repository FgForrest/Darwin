package one.edee.darwin.storage;

import java.util.Date;

/**
 * Interface for storing lock information for Locker class.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 * @version $Id$
 */
public interface LockPersister {

	/**
	 * Returns current database time, that is shared among nodes.
	 */
	Date getCurrentDatabaseTime();

	/**
	 * Method returns data about existing / nonexistent lock.
	 *
	 * @return 0 when there is no lock
	 *         1 when there is valid lock
	 *         2 when there is expired lock
	 */
	int getProcessLock(String processName, Date currentDate);

	/**
	 * Method stores lock.
	 * @param processName name of the process we want to lock
	 * @param until date until lock should be kept providing no one has unlock it by then
	 * @param unlockKey unlock key to be stored with lock
	 */
	void createLock(String processName, Date until, String unlockKey);

	/**
	 * Method releases lock.
	 * @param processName name of the process we want to unlock
	 * @param unlockKey key for unlocking the process
	 * @return 0 when there is no matching lock
	 *         1 when unlocking was performed successfully
	 */
	int releaseProcess(String processName, String unlockKey);

	/**
	 * Renews lease date for particular process, if you have correct unlock key (otherwise exception is thrown)
	 * @return 0 if update failed
	 *         1 if lease date was updated
	 */
	int renewLease(String processName, String unlockKey, Date until);

}
