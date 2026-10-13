/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder_client.security.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * @author danil
 */
@ConfigurationProperties(prefix = "security.jwt.rsa")
public record SecurityJwtProperties(
        String publicKey,
        String cookieName,
        Boolean cookieSecure,
        Duration cookieMaxAge
) {
    public SecurityJwtProperties {
        if (cookieName == null || cookieName.isBlank()) {
            cookieName = "ACCESS_TOKEN";
        }
        if (cookieSecure == null) {
            cookieSecure = true;
        }
        if (cookieMaxAge == null) {
            cookieMaxAge = Duration.ofMinutes(15);
        }
    }
}
