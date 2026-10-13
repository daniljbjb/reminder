package dan.reminder_client.exceptions;

public class ReminderServiceException extends RuntimeException {

    public ReminderServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
