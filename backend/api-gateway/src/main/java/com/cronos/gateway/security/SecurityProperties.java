package com.cronos.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(String jwtSecret, String internalToken) {

    public SecurityProperties {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 64) {
            throw new IllegalStateException("JWT_SECRET must be set and at least 64 bytes long (HS512)");
        }
        if (internalToken == null || internalToken.length() < 32) {
            throw new IllegalStateException("INTERNAL_API_TOKEN must be set and at least 32 characters long");
        }
    }
}
