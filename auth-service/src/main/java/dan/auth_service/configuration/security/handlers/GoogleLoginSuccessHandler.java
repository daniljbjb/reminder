/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.auth_service.configuration.security.handlers;

import dan.auth_service.model.User;
import dan.auth_service.configuration.security.JwtIssuer;
import dan.auth_service.controller.dto.JwtIssue;
import dan.auth_service.service.OneTimeCodeService;
import dan.auth_service.service.UserAccountService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author danil
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserAccountService userAccountService;
    private final JwtIssuer jwtIssuer;
    private final OneTimeCodeService oneTimeCodeService;

    @Value("${security.oauth.client-callback-url}")
    private String clientCallbackUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OidcUser oidcUser = extractOidcUser(authentication);

        String email = oidcUser.getEmail();
        String sub = oidcUser.getSubject();

        User user = userAccountService.getOrCreateByGoogle(sub, email);

        JwtIssue jwtIssue = jwtIssuer.issueAccessToken(user);

        // В идеале: code живёт 30-60 секунд и одноразовый
        String code = oneTimeCodeService.createCodeForJwt(jwtIssue);

        String redirectUrl = UriComponentsBuilder.fromUriString(clientCallbackUrl)
                .queryParam("code", code)
                .build(true)
                .toUriString();

        log.debug("OAuth2 success for userId={}, redirecting to client callback", user.getId());
        response.sendRedirect(redirectUrl);
    }

    private OidcUser extractOidcUser(Authentication authentication) throws ServletException {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) return oidcUser;
        throw new ServletException("Expected OidcUser principal, but got: " + principal.getClass());
    }
}
