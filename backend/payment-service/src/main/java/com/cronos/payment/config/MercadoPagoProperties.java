package com.cronos.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mercadopago")
public record MercadoPagoProperties(
        String accessToken,
        String webhookSecret,
        String apiBaseUrl,
        String frontendUrl,
        String successUrl,
        String failureUrl,
        String pendingUrl,
        String webhookUrl,
        long webhookMaxAgeSeconds
) {
    public boolean isConfigured() {
        return accessToken != null && !accessToken.isBlank();
    }

    public boolean hasWebhookSecret() {
        return webhookSecret != null && !webhookSecret.isBlank();
    }

    public String returnUrl(String kind) {
        String explicit = switch (kind) {
            case "success" -> successUrl;
            case "failure" -> failureUrl;
            default -> pendingUrl;
        };
        if (explicit != null && !explicit.isBlank()) {
            return explicit;
        }
        String base = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        return base + "/checkout/" + kind;
    }
}
