package com.cronos.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Fixed-window limit per client address on the credential endpoints. In-memory: one gateway instance. */
@Component
public class AuthRateLimitFilter implements GlobalFilter, Ordered {

    private static final Set<String> LIMITED = Set.of("/api/auth/login", "/api/auth/register", "/api/auth/refresh");
    private static final long WINDOW_MILLIS = 60_000;
    private static final int MAX_TRACKED_CLIENTS = 50_000;

    private record Window(long start, int count) {}

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int limit;
    private final int trustedProxyCount;

    public AuthRateLimitFilter(
            @Value("${app.rate-limit.auth-requests-per-minute}") int limit,
            @Value("${app.rate-limit.trusted-proxy-count}") int trustedProxyCount) {
        this.limit = limit;
        this.trustedProxyCount = trustedProxyCount;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (!HttpMethod.POST.equals(request.getMethod()) || !LIMITED.contains(request.getPath().value())) {
            return chain.filter(exchange);
        }
        long now = System.currentTimeMillis();
        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.values().removeIf(w -> now - w.start() >= WINDOW_MILLIS);
        }
        Window window = windows.merge(clientAddress(request), new Window(now, 1),
                (old, fresh) -> now - old.start() >= WINDOW_MILLIS ? fresh : new Window(old.start(), old.count() + 1));
        if (window.count() > limit) {
            long retryAfter = Math.max(1, (WINDOW_MILLIS - (now - window.start())) / 1000);
            exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(retryAfter));
            return Errors.write(exchange, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Too many attempts. Try again later");
        }
        return chain.filter(exchange);
    }

    private String clientAddress(ServerHttpRequest request) {
        if (trustedProxyCount > 0) {
            List<String> forwarded = request.getHeaders().getValuesAsList("X-Forwarded-For");
            if (forwarded.size() >= trustedProxyCount) {
                return forwarded.get(forwarded.size() - trustedProxyCount).trim();
            }
        }
        InetSocketAddress remote = request.getRemoteAddress();
        return remote == null ? "unknown" : remote.getAddress().getHostAddress();
    }
}
