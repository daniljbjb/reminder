package dan.reminder.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "reminder.email.retry")
public record EmailRetryProperties(
        @DefaultValue("3") int maxAttempts,
        @DefaultValue("PT1M") Duration initialDelay) {

    public EmailRetryProperties {
        if (maxAttempts < 1 || maxAttempts > 10) {
            throw new IllegalArgumentException("reminder.email.retry.max-attempts must be between 1 and 10");
        }
        if (initialDelay == null || initialDelay.isZero() || initialDelay.isNegative()) {
            throw new IllegalArgumentException("reminder.email.retry.initial-delay must be positive");
        }
    }
}
