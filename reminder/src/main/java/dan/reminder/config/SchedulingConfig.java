package dan.reminder.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({IdempotencyProperties.class, EmailRetryProperties.class})
public class SchedulingConfig {
}
