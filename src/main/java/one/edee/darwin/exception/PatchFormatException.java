package one.edee.darwin.exception;

/**
 * Exception is thrown when patch name is not in expected format.
 *
 * @author Radek Salay, FG Forest a.s. 6/20/16.
 */
public class PatchFormatException extends RuntimeException {
    private static final long serialVersionUID = -4291282664564936740L;

    public PatchFormatException(String message) {
        super(message);
    }

}
