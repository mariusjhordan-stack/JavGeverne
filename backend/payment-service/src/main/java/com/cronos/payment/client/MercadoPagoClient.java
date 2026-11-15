package com.cronos.payment.client;

import com.cronos.payment.config.MercadoPagoConfig;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.*;

@Component
public class MercadoPagoClient {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoClient.class);

    private final RestClient restClient;
    private final MercadoPagoConfig config;

    public MercadoPagoClient(
            @Qualifier("mercadoPagoRestClient") RestClient restClient,
            MercadoPagoConfig config) {
        this.restClient = restClient;
        this.config = config;
    }

    public record PreferenceResult(String preferenceId, String initPoint, String sandboxInitPoint) {}

    public Optional<PreferenceResult> createPreference(
            String externalReference,
            List<Map<String, Object>> items,
            String payerEmail,
            String notificationUrl) {

        if (!config.isConfigured()) {
            log.warn("Mercado Pago not configured - skipping preference creation");
            return Optional.empty();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("items", items);
        body.put("external_reference", externalReference);

        if (notificationUrl != null && !notificationUrl.isBlank()) {
            body.put("notification_url", notificationUrl);
        }

        if (payerEmail != null && !payerEmail.isBlank()) {
            body.put("payer", Map.of("email", payerEmail));
        }

        body.put("back_urls", Map.of(
                "success", "http://localhost:5173/checkout/success",
                "failure", "http://localhost:5173/checkout/failure",
                "pending", "http://localhost:5173/checkout/pending"
        ));
        body.put("auto_return", "approved");

        try {
            JsonNode response = restClient.post()
                    .uri("/checkout/preferences")
                    .header("Authorization", "Bearer " + config.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                log.error("Null response from Mercado Pago preference creation");
                return Optional.empty();
            }

            return Optional.of(new PreferenceResult(
                    response.path("id").asText(),
                    response.path("init_point").asText(),
                    response.path("sandbox_init_point").asText()
            ));
        } catch (RestClientException e) {
            log.error("Failed to create Mercado Pago preference: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public record PaymentInfo(
            String id,
            String status,
            String statusDetail,
            BigDecimal transactionAmount,
            String currencyId,
            String externalReference,
            String paymentMethodId,
            String payerEmail
    ) {}

    public Optional<PaymentInfo> getPayment(String paymentId) {
        if (!config.isConfigured()) {
            log.warn("Mercado Pago not configured - cannot query payment");
            return Optional.empty();
        }

        try {
            JsonNode response = restClient.get()
                    .uri("/v1/payments/{id}", paymentId)
                    .header("Authorization", "Bearer " + config.getAccessToken())
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                log.error("Null response querying payment {}", paymentId);
                return Optional.empty();
            }

            return Optional.of(new PaymentInfo(
                    response.path("id").asText(),
                    response.path("status").asText(),
                    response.path("status_detail").asText(),
                    response.path("transaction_amount").decimalValue(),
                    response.path("currency_id").asText(),
                    response.path("external_reference").asText(),
                    response.path("payment_method_id").asText(),
                    response.path("payer").path("email").asText()
            ));
        } catch (RestClientException e) {
            log.error("Failed to query Mercado Pago payment {}: {}", paymentId, e.getMessage());
            return Optional.empty();
        }
    }
}
