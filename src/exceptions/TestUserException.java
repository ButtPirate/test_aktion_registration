package exceptions;

/**
 * Created by Stef6 on 04/15/2018.
 */
public class TestUserException extends Throwable {
    public TestUserException() {
    }

    public TestUserException(String message) {
        super(message);
    }
}
