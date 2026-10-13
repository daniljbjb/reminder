package dan.reminder.exception;

import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.Getter;

@Getter
public class InvalidReminderTimeException extends RuntimeException {
    private final LocalDateTime localDateTime;
    private final ZoneId timezone;

    public InvalidReminderTimeException(String message, LocalDateTime localDateTime, ZoneId timezone) {
        super(message);
        this.localDateTime = localDateTime;
        this.timezone = timezone;
    }
}
