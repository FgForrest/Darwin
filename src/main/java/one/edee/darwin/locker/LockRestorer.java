package one.edee.darwin.locker;

/**
 * Provides information about process state. Allows to check whether the process already finished at any time from the outside
 * environment of the running process.
 *
 * @author Michal Kolesnac, FG Forrest a.s. (c) 2009
 */
public interface LockRestorer {

	/**
	 * Get process status. If process was finished return true else return false.
	 *
	 * @return true when process is finished
	 */
    boolean isFinished();
}
