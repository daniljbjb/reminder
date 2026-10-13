/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.auth_service.controller;

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import java.time.Duration;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 *
 * @author danil
 */
@RestController
@RequiredArgsConstructor
public class JwkSetController {

    private static final JWKSelector ALL_KEYS_SELECTOR
            = new JWKSelector(new JWKMatcher.Builder().build());

    private final JWKSource<SecurityContext> jwkSource;

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Map<String, Object>> jwks() {
        try {
            var jwks = jwkSource.get(ALL_KEYS_SELECTOR, null);
            var jwkSet = new JWKSet(jwks);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                    .body(jwkSet.toJSONObject());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
