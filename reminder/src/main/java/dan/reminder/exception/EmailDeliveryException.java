package dan.reminder.exception;

/** Indicates that the mail provider failed to deliver an email. */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
