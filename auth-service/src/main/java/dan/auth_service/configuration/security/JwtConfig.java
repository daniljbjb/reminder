/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.auth_service.configuration.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 *
 * @author danil
 */
@Configuration
public class JwtConfig {

    @Bean
    public RSAPublicKey rsaPublicKey(
            @Value("${security.jwt.rsa.public-key}") Resource resource
    ) throws IOException {
        String pem = resource.getContentAsString(StandardCharsets.UTF_8);
        return (RSAPublicKey) parsePublicKey(pem);
    }

    @Bean
    public RSAPrivateKey rsaPrivateKey(
            @Value("${security.jwt.rsa.private-key}") Resource resource
    ) throws IOException {
        String pem = resource.getContentAsString(StandardCharsets.UTF_8);
        return (RSAPrivateKey) parsePrivateKey(pem);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(
            RSAPublicKey publicKey,
            RSAPrivateKey privateKey,
            @Value("${security.jwt.rsa.kid:auth-rsa}") String kid
    ) {
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(kid)
                .build();

        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    private static PublicKey parsePublicKey(String pem) {
        try {
            byte[] der = decodePem(pem,
                    "-----BEGIN PUBLIC KEY-----",
                    "-----END PUBLIC KEY-----"
            );
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse RSA public key (X.509 / BEGIN PUBLIC KEY)", e);
        }
    }

    private static PrivateKey parsePrivateKey(String pem) {
        // Поддерживаем PKCS#8: BEGIN PRIVATE KEY
        if (pem.contains("BEGIN PRIVATE KEY")) {
            try {
                byte[] der = decodePem(pem,
                        "-----BEGIN PRIVATE KEY-----",
                        "-----END PRIVATE KEY-----"
                );
                KeyFactory kf = KeyFactory.getInstance("RSA");
                return kf.generatePrivate(new PKCS8EncodedKeySpec(der));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to parse RSA private key (PKCS#8 / BEGIN PRIVATE KEY)", e);
            }
        }

        // PKCS#1: BEGIN RSA PRIVATE KEY — твоим текущим кодом не поддерживается
        if (pem.contains("BEGIN RSA PRIVATE KEY")) {
            throw new IllegalStateException(
                    "Private key is in PKCS#1 format (BEGIN RSA PRIVATE KEY). "
                    + "Convert it to PKCS#8 (BEGIN PRIVATE KEY). Example: "
                    + "openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in rsa_pkcs1.pem -out rsa_pkcs8.pem"
            );
        }

        throw new IllegalStateException(
                "Unknown private key format. Expected BEGIN PRIVATE KEY (PKCS#8) "
                + "or BEGIN RSA PRIVATE KEY (PKCS#1)."
        );
    }

    private static byte[] decodePem(String pem, String begin, String end) {

        if (pem == null || pem.isBlank()) {
            throw new IllegalArgumentException("PEM is blank");
        }

        if (!pem.contains(begin) || !pem.contains(end)) {
            throw new IllegalArgumentException("PEM does not contain expected header/footer");
        }

        String normalized = pem
                .replace(begin, "")
                .replace(end, "")
                .replaceAll("\\s", "");

        return Base64.getDecoder().decode(normalized);
    }
}
