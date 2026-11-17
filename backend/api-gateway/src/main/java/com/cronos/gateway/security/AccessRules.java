package com.cronos.gateway.security;

import org.springframework.http.HttpMethod;

import java.util.Set;

/** Coarse authorization at the edge. Every service enforces its own rules again. */
public final class AccessRules {

    public enum Access { PUBLIC, AUTHENTICATED, ADMIN, NOT_FOUND, BAD_REQUEST }

    private static final Set<String> PUBLIC_AUTH_POSTS = Set.of(
            "/api/auth/login", "/api/auth/register", "/api/auth/refresh", "/api/auth/logout");

    private AccessRules() {}

    public static Access resolve(HttpMethod method, String rawPath) {
        String lower = rawPath.toLowerCase();
        if (lower.contains("..") || lower.contains(";") || lower.contains("\\") || lower.contains("//")
                || lower.contains("%2e") || lower.contains("%2f") || lower.contains("%5c") || lower.contains("%3b")) {
            return Access.BAD_REQUEST;
        }
        String path = rawPath.length() > 1 && rawPath.endsWith("/") ? rawPath.substring(0, rawPath.length() - 1) : rawPath;
        if (!path.startsWith("/api/") || path.startsWith("/internal") || path.contains("/internal/") || path.endsWith("/internal")) {
            return Access.NOT_FOUND;
        }
        if (path.startsWith("/api/admin/")) {
            return Access.ADMIN;
        }
        if (HttpMethod.POST.equals(method) && (PUBLIC_AUTH_POSTS.contains(path) || path.equals("/api/payments/webhook"))) {
            return Access.PUBLIC;
        }
        boolean read = HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method);
        if (read && (under(path, "/api/products") || under(path, "/api/brands") || under(path, "/api/categories"))) {
            return Access.PUBLIC;
        }
        return Access.AUTHENTICATED;
    }

    private static boolean under(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }
}
