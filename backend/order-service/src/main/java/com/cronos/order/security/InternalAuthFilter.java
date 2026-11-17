package com.cronos.order.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

/**
 * Trusts a caller only when it presents the internal token, which browsers never see: the gateway
 * strips it from incoming requests and adds its own. With a valid token, /internal/** is a
 * service-to-service call and everything else carries the end user the gateway took from the JWT.
 */
public class InternalAuthFilter extends OncePerRequestFilter {

    public static final String INTERNAL_TOKEN = "X-Internal-Token";
    public static final String USER_ID = "X-User-Id";
    public static final String USER_EMAIL = "X-User-Email";
    public static final String USER_ROLE = "X-User-Role";

    private final byte[] expectedToken;

    public InternalAuthFilter(String internalToken) {
        if (internalToken == null || internalToken.length() < 32) {
            throw new IllegalStateException("INTERNAL_API_TOKEN must be set and at least 32 characters long");
        }
        this.expectedToken = internalToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = request.getHeader(INTERNAL_TOKEN);
        if (token != null && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), expectedToken)) {
            Authentication authentication = request.getRequestURI().startsWith("/internal/")
                    ? new UsernamePasswordAuthenticationToken("service", null, List.of(new SimpleGrantedAuthority("ROLE_SERVICE")))
                    : endUser(request);
            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        chain.doFilter(request, response);
    }

    private Authentication endUser(HttpServletRequest request) {
        String id = request.getHeader(USER_ID);
        String role = request.getHeader(USER_ROLE);
        if (id == null || !("USER".equals(role) || "ADMIN".equals(role))) {
            return null;
        }
        try {
            RequestUser user = new RequestUser(UUID.fromString(id), request.getHeader(USER_EMAIL), role);
            return new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
