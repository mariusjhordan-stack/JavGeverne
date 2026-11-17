package com.cronos.gateway;

import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs the real gateway against a real HTTP downstream that records the headers it receives. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewaySecurityTest {

    static final String JWT_SECRET = "test-jwt-secret-for-gateway-tests-must-be-at-least-64-bytes-long-0123456789";
    static final String INTERNAL_TOKEN = "test-internal-token-0123456789abcdef0123";
    static final SecretKey KEY = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    static final Map<String, List<String>> received = new ConcurrentHashMap<>();
    static final HttpServer downstream = startDownstream();

    @Autowired
    WebTestClient client;

    static HttpServer startDownstream() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                received.clear();
                exchange.getRequestHeaders().forEach((k, v) -> received.put(k.toLowerCase(), v));
                received.put(":path", List.of(exchange.getRequestURI().getPath()));
                byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String url = "http://127.0.0.1:" + downstream.getAddress().getPort();
        for (String name : List.of("AUTH", "CATALOG", "ORDER", "PAYMENT", "NOTIFICATION")) {
            registry.add(name + "_SERVICE_URL", () -> url);
        }
        registry.add("JWT_SECRET", () -> JWT_SECRET);
        registry.add("INTERNAL_API_TOKEN", () -> INTERNAL_TOKEN);
        registry.add("AUTH_RATE_LIMIT_PER_MINUTE", () -> "5");
    }

    @AfterAll
    static void stop() {
        downstream.stop(0);
    }

    @BeforeEach
    void reset() {
        received.clear();
    }

    static final String USER_ID = UUID.randomUUID().toString();

    static String token(String role, String type, long ttlMillis, SecretKey key) {
        return Jwts.builder().subject(role.toLowerCase() + "@cronos.test")
                .claims(Map.of("role", role, "userId", USER_ID, "type", type))
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + ttlMillis))
                .signWith(key, Jwts.SIG.HS512).compact();
    }

    static String userToken() { return token("USER", "access", 60_000, KEY); }
    static String adminToken() { return token("ADMIN", "access", 60_000, KEY); }

    String header(String name) {
        List<String> values = received.get(name.toLowerCase());
        return values == null ? null : values.get(0);
    }

    @Test
    void anonymousCanReadCatalog_andNoIdentityIsForwarded() {
        client.get().uri("/api/products").exchange().expectStatus().isOk();
        assertThat(header("X-User-Id")).isNull();
        assertThat(header("X-User-Role")).isNull();
        assertThat(header("X-Internal-Token")).isEqualTo(INTERNAL_TOKEN);
    }

    @Test
    void clientSuppliedIdentityHeadersAreDiscarded() {
        client.get().uri("/api/products")
                .header("X-User-Id", UUID.randomUUID().toString())
                .header("X-User-Role", "ADMIN")
                .header("X-User-Email", "attacker@evil.test")
                .header("X-Internal-Token", "guessed")
                .exchange().expectStatus().isOk();
        assertThat(header("X-User-Id")).isNull();
        assertThat(header("X-User-Role")).isNull();
        assertThat(header("X-User-Email")).isNull();
        assertThat(received.get("x-internal-token")).containsExactly(INTERNAL_TOKEN);
    }

    @Test
    void forgedRoleHeaderDoesNotGrantAdmin() {
        client.post().uri("/api/admin/products").header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{}")
                .exchange().expectStatus().isUnauthorized();
        assertThat(received).isEmpty();
    }

    @Test
    void protectedRouteRequiresToken() {
        client.get().uri("/api/orders").exchange().expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.code").isEqualTo("AUTH_REQUIRED");
        assertThat(received).isEmpty();
    }

    @Test
    void invalidTokensAreRejected() {
        SecretKey otherKey = Keys.hmacShaKeyFor("another-secret-key-that-is-also-long-enough-for-hs512-signing-0123456789".getBytes(StandardCharsets.UTF_8));
        String unsigned = Jwts.builder().subject("x@x").claims(Map.of("role", "ADMIN", "userId", USER_ID, "type", "access")).compact();
        List<String> bad = List.of(
                "garbage",
                token("USER", "access", -1_000, KEY),
                token("USER", "refresh", 60_000, KEY),
                token("ADMIN", "access", 60_000, otherKey),
                token("SUPERUSER", "access", 60_000, KEY),
                unsigned);
        for (String t : bad) {
            client.get().uri("/api/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + t)
                    .exchange().expectStatus().isUnauthorized().expectBody().jsonPath("$.code").isEqualTo("TOKEN_INVALID");
        }
        assertThat(received).isEmpty();
    }

    @Test
    void identityComesFromTheTokenEvenIfClientSendsOtherHeaders() {
        client.get().uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken())
                .header("X-User-Id", UUID.randomUUID().toString())
                .header("X-User-Role", "ADMIN")
                .exchange().expectStatus().isOk();
        assertThat(received.get("x-user-id")).containsExactly(USER_ID);
        assertThat(received.get("x-user-role")).containsExactly("USER");
        assertThat(header("X-User-Email")).isEqualTo("user@cronos.test");
        assertThat(header("Authorization")).isNull();
    }

    @Test
    void userCannotReachAdminRoutes_adminCan() {
        client.get().uri("/api/admin/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken())
                .exchange().expectStatus().isForbidden();
        assertThat(received).isEmpty();

        client.get().uri("/api/admin/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .exchange().expectStatus().isOk();
        assertThat(header("X-User-Role")).isEqualTo("ADMIN");
    }

    @Test
    void catalogWritesRequireAuthentication() {
        client.put().uri("/api/products/" + UUID.randomUUID() + "/reviews/mine")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{}")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void internalPathsAreNeverRouted() {
        for (String path : List.of("/internal/stock/reservations", "/api/orders/internal/x", "/api/internal/orders")) {
            client.post().uri(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                    .contentType(MediaType.APPLICATION_JSON).bodyValue("{}")
                    .exchange().expectStatus().isNotFound();
        }
        assertThat(received).isEmpty();
    }

    @Test
    void pathTricksAreRejected() {
        client.get().uri(b -> b.path("/api/products/..;/admin/orders").build())
                .exchange().expectStatus().isBadRequest();
        assertThat(received).isEmpty();
    }

    @Test
    void webhookIsReachableWithoutToken() {
        client.post().uri("/api/payments/webhook?data.id=1&type=payment")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"type\":\"payment\"}")
                .exchange().expectStatus().isOk();
        assertThat(header("X-User-Id")).isNull();
    }

    @Test
    void authorizationHeaderIsForwardedOnlyToAuthService() {
        String t = userToken();
        client.get().uri("/api/auth/profile").header(HttpHeaders.AUTHORIZATION, "Bearer " + t)
                .exchange().expectStatus().isOk();
        assertThat(header("Authorization")).isEqualTo("Bearer " + t);
    }

    @Test
    void responsesCarrySecurityHeaders() {
        client.get().uri("/api/products").exchange()
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().valueEquals("X-Frame-Options", "DENY");
    }

    @Test
    void loginIsRateLimited() {
        int limited = 0;
        for (int i = 0; i < 8; i++) {
            int status = client.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"email\":\"a@b.c\",\"password\":\"x\"}").exchange().returnResult(String.class).getStatus().value();
            if (status == 429) limited++;
        }
        assertThat(limited).isEqualTo(3);
    }

    @Test
    void corsRejectsUnknownOrigins() {
        client.options().uri("/api/products").header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "GET").exchange().expectStatus().isForbidden();
        client.options().uri("/api/products").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET").exchange().expectStatus().isOk();
    }
}
