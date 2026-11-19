package com.cronos.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(
        String secretKey,
        String webhookSecret,
        String apiBaseUrl,
        String frontendUrl,
        String successUrl,
        String cancelUrl,
        long webhookMaxAgeSeconds
) {
    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank();
    }

    public boolean hasWebhookSecret() {
        return webhookSecret != null && !webhookSecret.isBlank();
    }

    public String returnUrl(String kind) {
        String explicit = "success".equals(kind) ? successUrl : cancelUrl;
        if (explicit != null && !explicit.isBlank()) {
            return explicit;
        }
        String base = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        return base + "/checkout/" + kind;
    }
}
