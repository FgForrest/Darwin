package one.edee.darwin.exception;

/**
 * @author Radek Salay, FG Forest a.s. 6/20/16.
 */
public class SqlCommandFormatException extends RuntimeException {

    public SqlCommandFormatException() {
        super();
    }

    public SqlCommandFormatException(String message) {
        super(message);
    }
}
