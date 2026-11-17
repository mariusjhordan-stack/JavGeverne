package com.cronos.order.service;

import com.cronos.order.entity.OrderStatus;
import com.cronos.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.UUID;

/** Cancels orders whose payment never arrived so their reserved stock goes back on sale. */
@Component
@ConditionalOnProperty(name = "app.orders.expiry-job-enabled", havingValue = "true", matchIfMissing = true)
public class OrderExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(OrderExpiryJob.class);

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final long paymentPendingTtlHours;

    public OrderExpiryJob(OrderRepository orderRepository, OrderService orderService,
                          @Value("${app.orders.payment-pending-ttl-hours:72}") long paymentPendingTtlHours) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.paymentPendingTtlHours = paymentPendingTtlHours;
    }

    @Scheduled(fixedDelayString = "${app.orders.expiry-interval-ms:60000}", initialDelayString = "${app.orders.expiry-interval-ms:60000}")
    public void run() {
        LocalDateTime now = LocalDateTime.now();
        for (UUID id : orderRepository.findExpired(EnumSet.of(OrderStatus.PENDING, OrderStatus.PAYMENT_FAILED), now)) {
            expire(id);
        }
        for (UUID id : orderRepository.findStale(OrderStatus.PAYMENT_PENDING, now.minusHours(paymentPendingTtlHours))) {
            expire(id);
        }
        for (UUID id : orderRepository.findPendingStockRelease()) {
            orderService.releaseStock(id);
        }
    }

    private void expire(UUID id) {
        try {
            orderService.cancel(id, "system", "EXPIRED");
            log.info("Order {} expired without payment", id);
        } catch (RuntimeException e) {
            log.warn("Could not expire order {}: {}", id, e.getMessage());
        }
    }
}
