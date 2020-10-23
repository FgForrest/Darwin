package one.edee.darwin.exception;

/**
 * Exception is thrown, when process tries to leaseLock that was leased by another thread, and is not released yet.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 * @version $Id$
 */
public class ProcessIsLockedException extends Exception {

	public ProcessIsLockedException() {
		super();
	}

	public ProcessIsLockedException(String message) {
		super(message);
	}

	public ProcessIsLockedException(Throwable cause) {
		super(cause);
	}

	public ProcessIsLockedException(String message, Throwable cause) {
		super(message, cause);
	}
}
