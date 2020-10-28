package one.edee.darwin.exception;

/**
 * Exception is thrown, when process tries to lease lock that was leased by another thread, and is not released yet.
 * This exception is expected and external code must cope with it.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
public class ProcessIsLockedException extends Exception {
	private static final long serialVersionUID = -1510599890915723448L;

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
