package com.cronos.gateway.security;

import com.cronos.gateway.security.AccessRules.Access;
import com.cronos.gateway.security.JwtVerifier.Identity;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * The only place where end-user identity enters the system: it is taken from a verified JWT and
 * forwarded as X-User-* headers together with the internal token. Anything the client sent under
 * those names is discarded first.
 */
@Component
public class IdentityGatewayFilter implements GlobalFilter, Ordered {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_EMAIL = "X-User-Email";
    public static final String USER_ROLE = "X-User-Role";
    public static final String INTERNAL_TOKEN = "X-Internal-Token";
    private static final List<String> RESERVED = List.of(USER_ID, USER_EMAIL, USER_ROLE, INTERNAL_TOKEN);

    private final JwtVerifier jwtVerifier;
    private final SecurityProperties properties;

    public IdentityGatewayFilter(JwtVerifier jwtVerifier, SecurityProperties properties) {
        this.jwtVerifier = jwtVerifier;
        this.properties = properties;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        Access access = AccessRules.resolve(request.getMethod(), path);

        if (access == Access.BAD_REQUEST) {
            return Errors.write(exchange, HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Malformed request path");
        }
        if (access == Access.NOT_FOUND) {
            return Errors.write(exchange, HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found");
        }

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        boolean bearer = authorization != null && authorization.startsWith("Bearer ");
        Optional<Identity> identity = bearer ? jwtVerifier.verify(authorization.substring(7)) : Optional.empty();

        if (access != Access.PUBLIC && identity.isEmpty()) {
            return Errors.write(exchange, HttpStatus.UNAUTHORIZED,
                    bearer ? "TOKEN_INVALID" : "AUTH_REQUIRED",
                    bearer ? "Session expired or token invalid" : "Authentication required");
        }
        if (access == Access.ADMIN && !"ADMIN".equals(identity.get().role())) {
            return Errors.write(exchange, HttpStatus.FORBIDDEN, "FORBIDDEN", "Administrator role required");
        }

        boolean toAuthService = path.startsWith("/api/auth/") || path.startsWith("/api/admin/users");
        ServerHttpRequest mutated = request.mutate().headers(headers -> {
            RESERVED.forEach(headers::remove);
            if (!toAuthService) {
                headers.remove(HttpHeaders.AUTHORIZATION);
            }
            headers.set(INTERNAL_TOKEN, properties.internalToken());
            identity.ifPresent(id -> {
                headers.set(USER_ID, id.userId());
                headers.set(USER_EMAIL, id.email());
                headers.set(USER_ROLE, id.role());
            });
        }).build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }
}
