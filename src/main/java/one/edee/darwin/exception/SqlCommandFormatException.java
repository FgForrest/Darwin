package one.edee.darwin.exception;

/**
 *
 *
 * @author Radek Salay, FG Forest a.s. 6/20/16.
 */
public class SqlCommandFormatException extends RuntimeException {
    private static final long serialVersionUID = -3096388796443679928L;

    public SqlCommandFormatException() {
        super();
    }

    public SqlCommandFormatException(String message) {
        super(message);
    }
}
