package dan.reminder_client.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record SecurityJwtValidationProperties(
        String issuer,
        String audience
) {
    public SecurityJwtValidationProperties {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("security.jwt.issuer is blank");
        }
        if (audience == null || audience.isBlank()) {
            throw new IllegalArgumentException("security.jwt.audience is blank");
        }
    }
}
