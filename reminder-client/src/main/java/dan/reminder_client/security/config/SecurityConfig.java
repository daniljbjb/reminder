/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder_client.security.config;

import dan.reminder_client.security.keys.PemKeys;
import dan.reminder_client.security.token.CookieBearerTokenResolver;
import java.security.interfaces.RSAPublicKey;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 *
 * @author danil
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({
    SecurityJwtProperties.class,
    SecurityJwtValidationProperties.class
})
public class SecurityConfig {

    @Bean
    SecurityFilterChain security(HttpSecurity http,
            BearerTokenResolver bearerTokenResolver,
            JwtDecoder jwtDecoder,
            AuthenticationEntryPoint authenticationEntryPoint) throws Exception {

        return http
                .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers(
                        "/client/reminder/auth/callback"
                )
                )
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/client/reminder/auth/callback",
                        "/client/reminder/main/bytitle",
                        "/css/**",
                        "/js/**",
                        "/images/**"
                ).permitAll()
                .anyRequest().authenticated()
                )
                .oauth2ResourceServer(rs -> rs
                .bearerTokenResolver(bearerTokenResolver)
                .jwt(jwt -> jwt.decoder(jwtDecoder))
                .authenticationEntryPoint(authenticationEntryPoint)
                )
                .logout(logout -> logout
                        .logoutUrl("/client/reminder/logout")
                        .deleteCookies("ACCESS_TOKEN", "XSRF-TOKEN")
                        .logoutSuccessUrl("/client/reminder/main/bytitle")
                )
                .build();
    }

    @Bean
    BearerTokenResolver bearerTokenResolver(SecurityJwtProperties props) {
        return new CookieBearerTokenResolver(props.cookieName());
    }

    @Bean
    JwtDecoder jwtDecoder(SecurityJwtProperties props,
            SecurityJwtValidationProperties validationProps) {
        RSAPublicKey publicKey = PemKeys.parsePublicKey(props.publicKey());
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(validationProps.issuer()),
                audienceValidator(validationProps.audience())
        );

        decoder.setJwtValidator(validator);
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> audienceValidator(String requiredAudience) {
        return jwt -> {
            if (jwt.getAudience().contains(requiredAudience)) {
                return OAuth2TokenValidatorResult.success();
            }

            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(
                            "invalid_token",
                            "Required audience is missing",
                            null
                    )
            );
        };
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint(SecurityJwtProperties props) {
        return (request, response, authException) -> {
            ResponseCookie deleteCookie = ResponseCookie.from(props.cookieName(), "")
                    .httpOnly(true)
                    .secure(props.cookieSecure())
                    .path("/")
                    .sameSite("Lax")
                    .maxAge(0)
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
            response.sendRedirect("/client/reminder/main/bytitle");
        };
    }
}
