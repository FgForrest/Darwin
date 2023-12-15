package one.edee.darwin.storage;

import one.edee.darwin.model.LockState;

import java.time.LocalDateTime;

/**
 * Interface for storing lock information for {@link one.edee.darwin.locker.Locker} class.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
public interface LockStorage {

	/**
	 * Returns current database time, that is shared among nodes.
	 */
	LocalDateTime getCurrentDatabaseTime();

	/**
	 * Method returns all existing process locks for specific instance.
	 */
	int releaseProcessesForInstance(String instanceId);

	/**
	 * Method returns data about existing / nonexistent lock.
	 */
	LockState getProcessLock(String processName, LocalDateTime currentDate);

	/**
	 * Method stores lock.
	 * @param processName name of the process we want to lock
	 * @param until date until lock should be kept providing no one has unlock it by then
	 * @param unlockKey unlock key to be stored with lock
	 */
	LockState createLock(String processName, LocalDateTime until, String unlockKey);

	/**
	 * Method releases lock.
	 * @param processName name of the process we want to unlock
	 * @param unlockKey key for unlocking the process
	 * @throws IllegalStateException when no lock for the process was found
	 */
	LockState releaseProcess(String processName, String unlockKey) throws IllegalStateException;

	/**
	 * Renews lease date for particular process, if you have correct unlock key (otherwise exception is thrown)
	 */
	LockState renewLease(String processName, String unlockKey, LocalDateTime until);

}
