/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.auth_service.configuration.security.handlers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author danil
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    @Value("${security.oauth.client-callback-url}")
    private String clientCallbackUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {

        String errorCode = extractErrorCode(exception);

        String redirectUrl = UriComponentsBuilder
                .fromUriString(clientCallbackUrl)
                .queryParam("error", errorCode)
                .build(true)
                .toUriString();

        log.debug("OAuth2 failure: error={}, redirecting to client callback", errorCode);
        response.sendRedirect(redirectUrl);
    }

    private static String extractErrorCode(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oae && oae.getError() != null) {
            String code = oae.getError().getErrorCode();
            return (code != null && !code.isBlank()) ? code : "oauth_failed";
        }
        return "oauth_failed";
    }
}
