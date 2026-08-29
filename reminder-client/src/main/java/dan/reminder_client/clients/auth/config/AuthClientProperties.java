/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder_client.clients.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * @author danil
 */
@ConfigurationProperties(prefix = "auth")
public record AuthClientProperties(
        String baseUrl,
        String clientId,
        String clientSecret,
        String exchangePath,
        Timeouts timeouts
        ) {

    public AuthClientProperties     {
        if (timeouts == null) {
            timeouts = new Timeouts();
        }
    }

    public record Timeouts(Duration connect, Duration read) {

        public Timeouts() {
            this(Duration.ofMillis(500), Duration.ofSeconds(2));
        }
    }
}
