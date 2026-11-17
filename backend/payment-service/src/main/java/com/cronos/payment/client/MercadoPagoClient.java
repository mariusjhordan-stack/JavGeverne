package com.cronos.payment.client;

import com.cronos.payment.config.MercadoPagoProperties;
import com.cronos.payment.security.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Thin client for the Mercado Pago REST API (Checkout Pro preferences and payments). */
@Component
public class MercadoPagoClient {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoClient.class);

    public record PreferenceResult(String preferenceId, String initPoint, String sandboxInitPoint) {}

    public record PaymentInfo(String id, String status, String statusDetail, BigDecimal transactionAmount,
                              String currencyId, String externalReference, String paymentMethodId, String payerEmail) {}

    private final RestClient client;
    private final MercadoPagoProperties properties;

    public MercadoPagoClient(@Qualifier("mercadoPagoRestClient") RestClient client, MercadoPagoProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public PreferenceResult createPreference(Map<String, Object> preference, String idempotencyKey) {
        try {
            JsonNode response = client.post().uri("/checkout/preferences")
                    .header("Authorization", "Bearer " + properties.accessToken())
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(preference)
                    .retrieve().body(JsonNode.class);
            if (response == null || response.path("id").asText("").isBlank()) {
                throw providerError("empty response", null);
            }
            return new PreferenceResult(response.path("id").asText(),
                    response.path("init_point").asText(null), response.path("sandbox_init_point").asText(null));
        } catch (RestClientResponseException e) {
            log.error("Mercado Pago rejected the preference: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw providerError("status " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw providerError(e.getMessage(), e);
        }
    }

    /** Empty when the provider does not know the payment; throws when the provider cannot be reached. */
    public Optional<PaymentInfo> getPayment(String paymentId) {
        try {
            JsonNode response = client.get().uri("/v1/payments/{id}", paymentId)
                    .header("Authorization", "Bearer " + properties.accessToken())
                    .retrieve().body(JsonNode.class);
            return response == null ? Optional.empty() : Optional.of(toInfo(response));
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw providerError("status " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw providerError(e.getMessage(), e);
        }
    }

    /** Payments the provider holds for an order, oldest first. */
    public List<PaymentInfo> findByExternalReference(String externalReference) {
        try {
            JsonNode response = client.get().uri(uri -> uri.path("/v1/payments/search")
                            .queryParam("external_reference", externalReference)
                            .queryParam("sort", "date_created").queryParam("criteria", "asc").queryParam("limit", 30).build())
                    .header("Authorization", "Bearer " + properties.accessToken())
                    .retrieve().body(JsonNode.class);
            List<PaymentInfo> payments = new ArrayList<>();
            if (response != null) {
                response.path("results").forEach(node -> payments.add(toInfo(node)));
            }
            return payments;
        } catch (RestClientException e) {
            throw providerError(e.getMessage(), e);
        }
    }

    private static PaymentInfo toInfo(JsonNode node) {
        return new PaymentInfo(node.path("id").asText(), node.path("status").asText(), node.path("status_detail").asText(null),
                node.path("transaction_amount").isNumber() ? node.path("transaction_amount").decimalValue() : null,
                node.path("currency_id").asText(null), node.path("external_reference").asText(null),
                node.path("payment_method_id").asText(null), node.path("payer").path("email").asText(null));
    }

    private static ApiException providerError(String detail, Exception cause) {
        ApiException ex = new ApiException(HttpStatus.BAD_GATEWAY, "PAYMENT_PROVIDER_ERROR", "The payment provider is not responding. Try again shortly");
        if (cause != null) {
            ex.initCause(cause);
        }
        log.warn("Mercado Pago call failed: {}", detail);
        return ex;
    }
}
