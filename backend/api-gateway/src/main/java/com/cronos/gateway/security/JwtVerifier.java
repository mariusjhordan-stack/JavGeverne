package com.cronos.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtVerifier {

    public record Identity(String userId, String email, String role) {}

    private final SecretKey key;

    public JwtVerifier(SecurityProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.jwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public Optional<Identity> verify(String token) {
        try {
            Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            Claims claims = jws.getPayload();
            String userId = claims.get("userId", String.class);
            String role = claims.get("role", String.class);
            if (!"HS512".equals(jws.getHeader().getAlgorithm())
                    || !"access".equals(claims.get("type", String.class))
                    || claims.getSubject() == null || userId == null
                    || !("USER".equals(role) || "ADMIN".equals(role))) {
                return Optional.empty();
            }
            UUID.fromString(userId);
            return Optional.of(new Identity(userId, claims.getSubject(), role));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
