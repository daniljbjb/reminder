/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder_client.security.token;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

/**
 *
 * @author danil
 */
public class CookieBearerTokenResolver implements BearerTokenResolver {

    private final String cookieName;

    public CookieBearerTokenResolver(String cookieName) {
        if (cookieName == null || cookieName.isBlank()) {
            throw new IllegalArgumentException("cookieName is blank");
        }
        this.cookieName = cookieName;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        String uri = request.getRequestURI();

        if (isPublicPath(uri)) {
            return null;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie c : cookies) {
            if (!cookieName.equals(c.getName())) {
                continue;
            }

            String token = c.getValue();
            if (token == null) {
                return null;
            }

            token = token.trim();
            if (token.isEmpty()) {
                return null;
            }

            if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
                token = token.substring(7).trim();
            }

            return token.isEmpty() ? null : token;
        }

        return null;
    }

    private boolean isPublicPath(String uri) {
        if (uri == null) {
            return false;
        }

        return uri.equals("/client/reminder/login")
                || uri.equals("/client/reminder/auth/callback")
                || uri.startsWith("/css/")
                || uri.startsWith("/js/")
                || uri.startsWith("/images/");
    }
}
