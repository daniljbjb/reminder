package dan.reminder.service;

import dan.reminder.exception.InvalidReminderTimeException;
import dan.reminder.exception.InvalidTimezoneException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimezoneResolverTest {

    private final TimezoneResolver resolver = new TimezoneResolver();

    @Test
    void resolveOrDefault_shouldUseAlmatyWhenHeaderIsMissing() {
        assertThat(resolver.resolveOrDefault(null)).isEqualTo(ZoneId.of("Asia/Almaty"));
        assertThat(resolver.resolveOrDefault("Asia/Qyzylorda"))
                .isEqualTo(ZoneId.of("Asia/Qyzylorda"));
    }

    @Test
    void resolveRequired_shouldRejectUnknownTimezone() {
        assertThatThrownBy(() -> resolver.resolveRequired("Asia/Unknown"))
                .isInstanceOf(InvalidTimezoneException.class);
    }

    @Test
    void toUnambiguousInstant_shouldRejectDstGap() {
        LocalDateTime missingTime = LocalDateTime.of(2026, 3, 29, 2, 30);

        assertThatThrownBy(() -> resolver.toUnambiguousInstant(
                missingTime, ZoneId.of("Europe/Berlin")))
                .isInstanceOf(InvalidReminderTimeException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void toUnambiguousInstant_shouldRejectDstOverlap() {
        LocalDateTime ambiguousTime = LocalDateTime.of(2026, 10, 25, 2, 30);

        assertThatThrownBy(() -> resolver.toUnambiguousInstant(
                ambiguousTime, ZoneId.of("Europe/Berlin")))
                .isInstanceOf(InvalidReminderTimeException.class)
                .hasMessageContaining("ambiguous");
    }
}
