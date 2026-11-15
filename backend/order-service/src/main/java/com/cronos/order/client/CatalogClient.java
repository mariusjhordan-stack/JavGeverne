package com.cronos.order.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Component
public class CatalogClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogClient.class);
    private final RestClient catalogRestClient;

    public CatalogClient(RestClient catalogRestClient) {
        this.catalogRestClient = catalogRestClient;
    }

    public boolean reserveStock(UUID productId, int quantity) {
        try {
            catalogRestClient.patch()
                    .uri("/api/products/{id}/stock", productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("quantity", -quantity))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Stock reserved: productId={}, quantity={}", productId, quantity);
            return true;
        } catch (Exception e) {
            log.error("Failed to reserve stock: productId={}, quantity={}, error={}", productId, quantity, e.getMessage());
            return false;
        }
    }

    public boolean releaseStock(UUID productId, int quantity) {
        try {
            catalogRestClient.patch()
                    .uri("/api/products/{id}/stock", productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("quantity", quantity))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Stock released: productId={}, quantity={}", productId, quantity);
            return true;
        } catch (Exception e) {
            log.error("Failed to release stock: productId={}, quantity={}, error={}", productId, quantity, e.getMessage());
            return false;
        }
    }
}
