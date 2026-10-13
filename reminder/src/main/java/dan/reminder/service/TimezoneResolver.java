package dan.reminder.service;

import dan.reminder.exception.InvalidReminderTimeException;
import dan.reminder.exception.InvalidTimezoneException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TimezoneResolver {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Almaty");

    public ZoneId resolveRequired(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return DEFAULT_ZONE;
        }
        return parse(timezone);
    }

    public ZoneId resolveOrDefault(String timezone) {
        return timezone == null || timezone.isBlank() ? DEFAULT_ZONE : parse(timezone);
    }

    public Instant toUnambiguousInstant(LocalDateTime localDateTime, ZoneId zoneId) {
        List<ZoneOffset> offsets = zoneId.getRules().getValidOffsets(localDateTime);
        if (offsets.size() != 1) {
            throw new InvalidReminderTimeException(
                    offsets.isEmpty()
                            ? "Local time does not exist in the specified timezone"
                            : "Local time is ambiguous in the specified timezone",
                    localDateTime,
                    zoneId);
        }
        return localDateTime.toInstant(offsets.get(0));
    }

    private ZoneId parse(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException ex) {
            throw new InvalidTimezoneException("Unknown timezone", timezone);
        }
    }
}
