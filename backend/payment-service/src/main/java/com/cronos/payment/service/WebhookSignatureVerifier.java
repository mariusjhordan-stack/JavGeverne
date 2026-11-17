package com.cronos.payment.service;

import com.cronos.payment.config.MercadoPagoProperties;
import com.cronos.payment.security.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Mercado Pago signs "id:{data.id};request-id:{x-request-id};ts:{ts};" with HMAC-SHA256 and sends
 * it as x-signature: ts=...,v1=.... Verification is mandatory: with no secret configured every
 * webhook is refused.
 */
@Component
public class WebhookSignatureVerifier {

    private static final long FUTURE_SKEW_SECONDS = 300;

    private final MercadoPagoProperties properties;

    public WebhookSignatureVerifier(MercadoPagoProperties properties) {
        this.properties = properties;
    }

    public void verify(String dataId, String signatureHeader, String requestId) {
        if (!properties.hasWebhookSecret()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "WEBHOOK_NOT_CONFIGURED", "Webhook signature secret is not configured");
        }
        if (dataId == null || dataId.isBlank() || signatureHeader == null || signatureHeader.isBlank()) {
            throw unauthorized("Missing signature");
        }
        String ts = null;
        String v1 = null;
        for (String part : signatureHeader.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length == 2 && pair[0].trim().equals("ts")) ts = pair[1].trim();
            if (pair.length == 2 && pair[0].trim().equals("v1")) v1 = pair[1].trim();
        }
        if (ts == null || v1 == null) {
            throw unauthorized("Malformed signature");
        }
        String manifest = "id:" + dataId.toLowerCase() + ";request-id:" + (requestId == null ? "" : requestId) + ";ts:" + ts + ";";
        byte[] expected = hmac(manifest, properties.webhookSecret());
        byte[] received;
        try {
            received = HexFormat.of().parseHex(v1);
        } catch (IllegalArgumentException e) {
            throw unauthorized("Malformed signature");
        }
        if (!MessageDigest.isEqual(expected, received)) {
            throw unauthorized("Signature mismatch");
        }
        checkTimestamp(ts);
    }

    private void checkTimestamp(String ts) {
        long value;
        try {
            value = Long.parseLong(ts);
        } catch (NumberFormatException e) {
            throw unauthorized("Malformed timestamp");
        }
        long seconds = value > 100_000_000_000L ? value / 1000 : value;
        long age = System.currentTimeMillis() / 1000 - seconds;
        if (age > properties.webhookMaxAgeSeconds() || age < -FUTURE_SKEW_SECONDS) {
            throw unauthorized("Signature timestamp outside the accepted window");
        }
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
