/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.auth_service.configuration.security;

import dan.auth_service.controller.dto.JwtIssue;
import dan.auth_service.model.User;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 *
 * @author danil
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtIssuer {

    private final JwtEncoder jwtEncoder;
    private final Clock clock;

    @Value("${security.jwt.issuer}")
    private String issuer;

    @Value("${security.jwt.access-ttl-seconds:600}")
    private long accessTtlSeconds;

    @Value("#{'${security.jwt.audience}'.split(',')}")
    private List<String> audience;

    public JwtIssue issueAccessToken(User user) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plusSeconds(accessTtlSeconds);

        List<String> normalizedAudience = audience.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();

        // sub = внешний стабильный идентификатор (publicId)
        String subject = user.getPublicId().toString();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(subject)
                .audience(normalizedAudience)
                .claim("roles", user.getRoles().stream().map(Enum::name).toList())
                .claim("email", user.getEmail())
                .build();

        // Можно не задавать header вообще — JwtEncoder сам выставит alg по ключу.
        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims))
                .getTokenValue();

        log.debug("Issued JWT sub={}", subject);
        return new JwtIssue(token, expiresAt);
    }
}
