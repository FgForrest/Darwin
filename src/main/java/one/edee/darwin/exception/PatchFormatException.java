package one.edee.darwin.exception;

/**
 * @author Radek Salay, FG Forest a.s. 6/20/16.
 */
public class PatchFormatException extends RuntimeException {
    public PatchFormatException() {
        super();
    }

    public PatchFormatException(String message) {
        super(message);
    }
}
