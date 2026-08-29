package dan.reminder_client.controller;

import dan.reminder_client.clients.auth.AuthClient;
import dan.reminder_client.exceptions.AuthServiceConfigurationException;
import dan.reminder_client.exceptions.AuthServiceRejectedException;
import dan.reminder_client.exceptions.AuthServiceTemporaryException;
import dan.reminder_client.security.config.SecurityJwtProperties;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/client/reminder/auth")
public class AuthController {

    private final AuthClient authClient;
    private final SecurityJwtProperties jwtProperties;

    @GetMapping("/callback")
    public String callback(@RequestParam("code") String code,
            HttpServletResponse response,
            Model model) {

        if (code == null || code.isBlank()) {
            model.addAttribute("authError", "Сервис входа вернул пустой одноразовый код.");
            model.addAttribute("authenticated", false);
            return "main";
        }

        try {
            String jwt = authClient.exchangeCodeForJwt(code);

            ResponseCookie cookie = ResponseCookie.from(jwtProperties.cookieName(), jwt)
                    .httpOnly(true)
                    .secure(jwtProperties.cookieSecure())
                    .path("/")
                    .sameSite("Lax")
                    .maxAge(jwtProperties.cookieMaxAge())
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            return "redirect:/client/reminder/main/bytitle";

        } catch (AuthServiceRejectedException ex) {
            log.warn("Authentication code exchange was rejected", ex);
            model.addAttribute("authError",
                    "Не удалось войти: одноразовый код недействителен, уже использован или истёк. Начните вход заново.");
            model.addAttribute("authenticated", false);
            return "main";

        } catch (AuthServiceConfigurationException ex) {
            log.error("Authentication service-to-service configuration error", ex);
            model.addAttribute("authError",
                    "Ошибка настройки входа: reminder-client не авторизован в auth-service. Проверьте AUTH_CLIENT_SECRET.");
            model.addAttribute("authenticated", false);
            return "main";

        } catch (AuthServiceTemporaryException ex) {
            log.warn("Authentication service is unavailable or failed", ex);
            model.addAttribute("authError",
                    "Сервис входа недоступен или вернул внутреннюю ошибку. Попробуйте позже.");
            model.addAttribute("authenticated", false);
            return "main";
        }
    }

    @GetMapping("/jwtpage")
    public String aboutJwt(Model model, Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            Map<String, Object> claims = new LinkedHashMap<>(jwt.getClaims());

            claims.put("iat", fmt.format(jwt.getIssuedAt().atZone(ZoneId.systemDefault())));
            claims.put("exp", fmt.format(jwt.getExpiresAt().atZone(ZoneId.systemDefault())));
            model.addAttribute("jwt", jwt.getTokenValue());

            Instant now = Instant.now();
            Instant issuedAt = jwt.getIssuedAt();
            Instant expiresAt = jwt.getExpiresAt();
            long ttlSeconds = Duration.between(issuedAt, expiresAt).getSeconds();
            long remainingSeconds = Math.max(0, Duration.between(now, expiresAt).getSeconds());

            model.addAttribute("ttlSeconds", ttlSeconds);
            model.addAttribute("remainingSeconds", remainingSeconds);
            model.addAttribute("claims", claims);
        }

        return "jwt-page";
    }
}
