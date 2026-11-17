package com.cronos.order.client;

import com.cronos.order.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/** Best effort: a notification that cannot be delivered never fails or rolls back an order operation. */
@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestClient client;

    public NotificationClient(@Qualifier("notificationRestClient") RestClient client) {
        this.client = client;
    }

    public void send(String type, Order order, String subject, String body) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", order.getUserId());
            payload.put("userEmail", order.getUserEmail());
            payload.put("type", type);
            payload.put("subject", subject);
            payload.put("body", body);
            payload.put("orderId", order.getId());
            client.post().uri("/internal/notifications").contentType(MediaType.APPLICATION_JSON)
                    .body(payload).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("Notification {} for order {} could not be sent: {}", type, order.getId(), e.getMessage());
        }
    }
}
