package com.cronos.payment.client;

import com.cronos.payment.security.ApiException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class OrderClient {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderItem(String productName, int quantity, BigDecimal unitPrice) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderSnapshot(UUID id, UUID userId, String userEmail, String status, BigDecimal total,
                                String currency, List<OrderItem> items, LocalDateTime expiresAt) {}

    private final RestClient client;

    public OrderClient(@Qualifier("orderServiceRestClient") RestClient client) {
        this.client = client;
    }

    public Optional<OrderSnapshot> getOrder(UUID orderId) {
        try {
            return Optional.ofNullable(client.get().uri("/internal/orders/{id}", orderId).retrieve().body(OrderSnapshot.class));
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw unavailable(e);
        } catch (RestClientException e) {
            throw unavailable(e);
        }
    }

    /** Throws when the order service did not acknowledge, so the caller can let the provider retry. */
    public void reportPaymentResult(UUID orderId, String result, String paymentId) {
        try {
            client.post().uri("/internal/orders/{id}/payment-result", orderId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("result", result, "paymentId", paymentId))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw unavailable(e);
        }
    }

    private static ApiException unavailable(Exception cause) {
        ApiException ex = new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ORDER_SERVICE_UNAVAILABLE", "Order service is temporarily unavailable");
        ex.initCause(cause);
        return ex;
    }
}
