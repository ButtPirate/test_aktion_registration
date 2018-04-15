package exceptions;

/**
 * Created by Stef6 on 04/15/2018.
 */
public class BackendException extends Throwable {
    public BackendException() {
    }

    public BackendException(String message) {
        super(message);
    }
}
