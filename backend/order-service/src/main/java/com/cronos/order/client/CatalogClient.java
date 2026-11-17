package com.cronos.order.client;

import com.cronos.order.security.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Talks to catalog-service's internal stock API. The catalog decides prices and availability. */
@Component
public class CatalogClient {

    public record Item(UUID productId, int quantity) {}

    public record ReservedLine(UUID productId, String name, String brandName, String imageUrl,
                               BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {}

    public record Reservation(UUID orderId, String currency, List<ReservedLine> items, BigDecimal total) {}

    private final RestClient client;
    private final ObjectMapper objectMapper;

    public CatalogClient(@Qualifier("catalogRestClient") RestClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public Reservation reserve(UUID orderId, List<Item> items) {
        try {
            return client.post().uri("/internal/stock/reservations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("orderId", orderId, "items", items))
                    .retrieve().body(Reservation.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 409 || e.getStatusCode().value() == 400) {
                String code = "STOCK_UNAVAILABLE";
                String message = "Some items are not available";
                try {
                    JsonNode body = objectMapper.readTree(e.getResponseBodyAsString());
                    code = body.path("code").asText(code);
                    message = body.path("error").asText(message);
                } catch (Exception ignored) {
                    // keep defaults
                }
                throw new ApiException(HttpStatus.valueOf(e.getStatusCode().value()), code, message);
            }
            throw unavailable(e);
        } catch (RestClientException e) {
            throw unavailable(e);
        }
    }

    public void release(UUID orderId) {
        try {
            client.post().uri("/internal/stock/reservations/{id}/release", orderId).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw unavailable(e);
        }
    }

    /** @return false when the catalog reports the reservation was already released. */
    public boolean confirm(UUID orderId) {
        try {
            client.post().uri("/internal/stock/reservations/{id}/confirm", orderId).retrieve().toBodilessEntity();
            return true;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 409 || e.getStatusCode().value() == 404) {
                return false;
            }
            throw unavailable(e);
        } catch (RestClientException e) {
            throw unavailable(e);
        }
    }

    private static ApiException unavailable(Exception cause) {
        ApiException ex = new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CATALOG_UNAVAILABLE", "Catalog is temporarily unavailable");
        ex.initCause(cause);
        return ex;
    }
}
