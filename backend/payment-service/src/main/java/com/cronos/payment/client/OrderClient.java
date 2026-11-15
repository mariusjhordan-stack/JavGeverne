package com.cronos.payment.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.UUID;

@Component
public class OrderClient {

    private static final Logger log = LoggerFactory.getLogger(OrderClient.class);

    private final RestClient restClient;

    public OrderClient(@Qualifier("orderServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public boolean updateOrderStatus(UUID orderId, String status) {
        try {
            restClient.put()
                    .uri("/api/orders/{id}/status", orderId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("status", status))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Updated order {} status to {}", orderId, status);
            return true;
        } catch (RestClientException e) {
            log.error("Failed to update order {} status to {}: {}", orderId, status, e.getMessage());
            return false;
        }
    }
}
