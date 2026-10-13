package dan.reminder.exception;

import lombok.Getter;

@Getter
public class InvalidTimezoneException extends RuntimeException {
    private final String timezone;

    public InvalidTimezoneException(String message, String timezone) {
        super(message);
        this.timezone = timezone;
    }
}
