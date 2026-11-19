package com.cronos.payment.client;

import com.cronos.payment.config.StripeProperties;
import com.cronos.payment.security.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class StripeClient {

    private static final Logger log = LoggerFactory.getLogger(StripeClient.class);

    private final RestClient client;
    private final StripeProperties properties;

    public StripeClient(@Qualifier("stripeRestClient") RestClient client, StripeProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public JsonNode createCheckoutSession(MultiValueMap<String, String> form, String idempotencyKey) {
        try {
            JsonNode response = client.post().uri("/v1/checkout/sessions")
                    .header("Authorization", "Bearer " + properties.secretKey())
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve().body(JsonNode.class);
            if (response == null || response.path("id").asText("").isBlank()
                    || response.path("url").asText("").isBlank()) {
                throw providerError("empty checkout session response", null);
            }
            return response;
        } catch (RestClientResponseException e) {
            throw providerError("status " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw providerError(e.getMessage(), e);
        }
    }

    public JsonNode getCheckoutSession(String sessionId) {
        try {
            JsonNode response = client.get().uri("/v1/checkout/sessions/{id}", sessionId)
                    .header("Authorization", "Bearer " + properties.secretKey())
                    .retrieve().body(JsonNode.class);
            if (response == null) {
                throw providerError("empty checkout session response", null);
            }
            return response;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return null;
            }
            throw providerError("status " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw providerError(e.getMessage(), e);
        }
    }

    public JsonNode getPaymentIntent(String paymentIntentId) {
        try {
            JsonNode response = client.get().uri("/v1/payment_intents/{id}", paymentIntentId)
                    .header("Authorization", "Bearer " + properties.secretKey())
                    .retrieve().body(JsonNode.class);
            if (response == null) {
                throw providerError("empty payment intent response", null);
            }
            return response;
        } catch (RestClientException e) {
            throw providerError(e.getMessage(), e);
        }
    }

    private static ApiException providerError(String detail, Exception cause) {
        ApiException ex = new ApiException(HttpStatus.BAD_GATEWAY, "PAYMENT_PROVIDER_ERROR",
                "The payment provider is not responding. Try again shortly");
        if (cause != null) {
            ex.initCause(cause);
        }
        log.warn("Stripe call failed: {}", detail);
        return ex;
    }
}
