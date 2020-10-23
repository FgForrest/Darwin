package one.edee.darwin.locker;

/**
 *  Provider information about process which want lock and can renew a lock. 
 *
 * @author Michal Kolesnac, FG Forrest a.s. (c) 2009
 * @version $Id$
 */
public interface LockRestorer {
	/**
	 * Get process status. If process was finished return true else return false;
	 * @return true or false
	 */
    boolean isFinished();
}
