package com.cronos.auth;

import com.cronos.auth.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "dGVzdC1qd3Qtc2VjcmV0LWtleS1mb3ItY3Jvbm9zLWdlbmV2ZS10ZXN0LWVudi0yMDI0LXNlY3VyZQ==";
    private static final long ACCESS_EXPIRATION = 3600000;
    private static final long REFRESH_EXPIRATION = 86400000;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, ACCESS_EXPIRATION, REFRESH_EXPIRATION);
    }

    @Test
    void generateAccessToken() {
        String userId = UUID.randomUUID().toString();
        String token = jwtService.generateAccessToken("user@test.com", "USER", userId);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(token.chars().filter(c -> c == '.').count() == 2);
    }

    @Test
    void generateRefreshToken() {
        String token = jwtService.generateRefreshToken("user@test.com");

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void extractEmailFromAccessToken() {
        String userId = UUID.randomUUID().toString();
        String token = jwtService.generateAccessToken("alice@test.com", "ADMIN", userId);

        assertEquals("alice@test.com", jwtService.extractEmail(token));
    }

    @Test
    void extractEmailFromRefreshToken() {
        String token = jwtService.generateRefreshToken("bob@test.com");

        assertEquals("bob@test.com", jwtService.extractEmail(token));
    }

    @Test
    void extractRoleFromAccessToken() {
        String userId = UUID.randomUUID().toString();
        String token = jwtService.generateAccessToken("admin@test.com", "ADMIN", userId);

        assertEquals("ADMIN", jwtService.extractRole(token));
    }

    @Test
    void extractUserIdFromAccessToken() {
        String userId = UUID.randomUUID().toString();
        String token = jwtService.generateAccessToken("user@test.com", "USER", userId);

        assertEquals(userId, jwtService.extractUserId(token));
    }

    @Test
    void accessTokenTypeIsAccess() {
        String token = jwtService.generateAccessToken("u@t.com", "USER", UUID.randomUUID().toString());

        assertTrue(jwtService.isAccessToken(token));
        assertFalse(jwtService.isRefreshToken(token));
    }

    @Test
    void refreshTokenTypeIsRefresh() {
        String token = jwtService.generateRefreshToken("u@t.com");

        assertTrue(jwtService.isRefreshToken(token));
        assertFalse(jwtService.isAccessToken(token));
    }

    @Test
    void validTokenReturnsTrue() {
        String token = jwtService.generateAccessToken("u@t.com", "USER", UUID.randomUUID().toString());

        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void expiredTokenReturnsFalse() {
        JwtService shortLived = new JwtService(SECRET, 0, 0);
        String token = shortLived.generateAccessToken("u@t.com", "USER", UUID.randomUUID().toString());

        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    void malformedTokenReturnsFalse() {
        assertFalse(jwtService.isTokenValid("not.a.jwt"));
        assertFalse(jwtService.isTokenValid(""));
        assertFalse(jwtService.isTokenValid("abc"));
    }

    @Test
    void tokenSignedWithDifferentKeyIsInvalid() {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode("b3RoZXIta2V5LWZvci10ZXN0aW5nLXB1cnBvc2VzLW11c3QtYmUtbG9uZy1lbm91Z2g="));

        String forged = Jwts.builder()
                .subject("hacker@test.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(otherKey)
                .compact();

        assertFalse(jwtService.isTokenValid(forged));
    }

    @Test
    void refreshTokenHasNoRole() {
        String token = jwtService.generateRefreshToken("u@t.com");

        assertNull(jwtService.extractRole(token));
    }
}
