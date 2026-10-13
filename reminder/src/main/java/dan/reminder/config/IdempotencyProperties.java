package dan.reminder.config;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "reminder.idempotency")
public record IdempotencyProperties(
        @DefaultValue("PT24H") Duration retention,
        @DefaultValue("PT1M") Duration cleanupInitialDelay,
        @DefaultValue("PT1H") Duration cleanupInterval) {

    public IdempotencyProperties {
        requirePositive("retention", retention);
        requirePositive("cleanup-initial-delay", cleanupInitialDelay);
        requirePositive("cleanup-interval", cleanupInterval);
    }

    private static void requirePositive(String property, Duration value) {
        Objects.requireNonNull(value, property + " must not be null");
        if (value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException(property + " must be positive");
        }
    }
}
