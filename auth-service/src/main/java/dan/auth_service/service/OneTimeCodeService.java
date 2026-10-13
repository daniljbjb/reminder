/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.auth_service.service;

import dan.auth_service.controller.dto.JwtIssue;
import dan.auth_service.exception.CodeExpiredException;
import dan.auth_service.exception.InvalidCodeException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author danil
 */
@Service
public class OneTimeCodeService {

    private final ConcurrentHashMap<String, Entry> storage = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration ttl;

    public OneTimeCodeService(
            Clock clock,
            @Value("${security.exchange.code-ttl-seconds:60}") long ttlSeconds
    ) {
        this.clock = clock;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public String createCodeForJwt(JwtIssue jwtIssue) {
        if (jwtIssue == null || jwtIssue.jwt() == null || jwtIssue.jwt().isBlank()) {
            throw new IllegalArgumentException("jwt is blank");
        }

        cleanupExpiredIfNeeded();

        String code = UUID.randomUUID().toString();
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(ttl);

        storage.put(code, new Entry(jwtIssue, expiresAt));
        return code;
    }

    public JwtIssue consumeJwtByCode(String code) {
        Entry entry = storage.remove(code); // одноразовость
        if (entry == null) {
            throw new InvalidCodeException("Invalid one-time code");
        }

        Instant now = Instant.now(clock);
        if (now.isAfter(entry.expiresAt())) {
            throw new CodeExpiredException("One-time code expired");
        }

        return entry.jwtIssue();
    }

    private void cleanupExpiredIfNeeded() {
        if (ThreadLocalRandom.current().nextInt(100) != 0) {
            return;
        }
        Instant now = Instant.now(clock);
        storage.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }

    private record Entry(JwtIssue jwtIssue, Instant expiresAt) {}

}
