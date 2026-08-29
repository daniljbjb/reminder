/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.auth_service.controller;

import dan.auth_service.controller.dto.CodeRequest;
import dan.auth_service.controller.dto.JwtIssue;
import dan.auth_service.controller.dto.TokenResponse;
import dan.auth_service.service.OneTimeCodeService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author danil
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthExchangeController {

    private final OneTimeCodeService oneTimeCodeService;
    private final Clock clock;

    @PostMapping("/exchange")
    public ResponseEntity<TokenResponse> exchange(@Valid @RequestBody CodeRequest request) {
       

        JwtIssue issue = oneTimeCodeService.consumeJwtByCode(request.code());

        long expiresIn = Math.max(
                0,
                Duration.between(Instant.now(clock), issue.expiresAt()).getSeconds()
        );

        return ResponseEntity.ok(
                new TokenResponse(issue.jwt(), expiresIn)
        );
    }
}
