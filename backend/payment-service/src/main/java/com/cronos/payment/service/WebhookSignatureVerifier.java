package com.cronos.payment.service;

import com.cronos.payment.config.StripeProperties;
import com.cronos.payment.security.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Component
public class WebhookSignatureVerifier {

    private final StripeProperties properties;

    public WebhookSignatureVerifier(StripeProperties properties) {
        this.properties = properties;
    }

    public void verify(String payload, String signatureHeader) {
        if (!properties.hasWebhookSecret()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "WEBHOOK_NOT_CONFIGURED", "Webhook signature secret is not configured");
        }
        if (payload == null || signatureHeader == null || signatureHeader.isBlank()) {
            throw unauthorized("Missing signature");
        }
        String timestamp = null;
        List<String> signatures = new ArrayList<>();
        for (String part : signatureHeader.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length == 2 && pair[0].trim().equals("t")) timestamp = pair[1].trim();
            if (pair.length == 2 && pair[0].trim().equals("v1")) signatures.add(pair[1].trim());
        }
        if (timestamp == null || signatures.isEmpty()) {
            throw unauthorized("Malformed signature");
        }
        long seconds;
        try {
            seconds = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw unauthorized("Malformed timestamp");
        }
        long age = System.currentTimeMillis() / 1000 - seconds;
        if (age > properties.webhookMaxAgeSeconds() || age < -300) {
            throw unauthorized("Signature timestamp outside the accepted window");
        }
        byte[] expected = hmac(timestamp + "." + payload, properties.webhookSecret());
        boolean valid = false;
        for (String signature : signatures) {
            try {
                valid |= MessageDigest.isEqual(expected, HexFormat.of().parseHex(signature));
            } catch (IllegalArgumentException ignored) {
                // Other v1 signatures in the same header may still be valid during key rotation.
            }
        }
        if (!valid) throw unauthorized("Signature mismatch");
    }

    public static byte[] hmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_SIGNATURE", message);
    }
}
