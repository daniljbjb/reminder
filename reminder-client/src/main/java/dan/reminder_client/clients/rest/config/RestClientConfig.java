/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder_client.clients.rest.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Arrays;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 *
 * @author danil
 */
@Configuration
@EnableConfigurationProperties(RemindersApiProperties.class)
public class RestClientConfig {

    @Bean
    RestClient restClient(RemindersApiProperties props) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {

                    String bearerToken = resolveBearerToken();
                    if (bearerToken != null) {
                        request.getHeaders().setBearerAuth(bearerToken);
                    }

                    String timezone = resolveTimezone();
                    request.getHeaders().set("X-Timezone", timezone);

                    return execution.execute(request, body);
                })
                .build();
    }

    private String resolveTimezone() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                String timezone = Arrays.stream(cookies)
                        .filter(cookie -> "CLIENT_TIMEZONE".equals(cookie.getName()))
                        .map(Cookie::getValue)
                        .findFirst()
                        .orElse(null);
                if (timezone != null) {
                    try {
                        return ZoneId.of(timezone).getId();
                    } catch (DateTimeException ignored) {
                        // Ignore a malformed client cookie and use a safe fallback.
                    }
                }
            }
        }
        return "UTC";
    }

    private String resolveBearerToken() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getTokenValue();
        }

        return null;
    }
}
