package com.cronos.order.service;

import com.cronos.order.client.CatalogClient;
import com.cronos.order.client.NotificationClient;
import com.cronos.order.dto.CreateOrderRequest;
import com.cronos.order.dto.OrderResponse;
import com.cronos.order.entity.Order;
import com.cronos.order.entity.OrderItem;
import com.cronos.order.entity.OrderStatus;
import com.cronos.order.repository.OrderRepository;
import com.cronos.order.repository.OrderStatusHistoryRepository;
import com.cronos.order.security.ApiException;
import com.cronos.order.security.RequestUser;
import com.cronos.order.service.OrderStateService.Change;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final Set<OrderStatus> ADMIN_TARGETS =
            EnumSet.of(OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED, OrderStatus.CANCELLED);
    public static final Set<OrderStatus> PURCHASED = EnumSet.of(
            OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    public enum PaymentResult { APPROVED, REJECTED, PENDING, REFUNDED }

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OrderStateService state;
    private final CatalogClient catalog;
    private final NotificationClient notifications;
    private final long reservationTtlMinutes;

    public OrderService(OrderRepository orderRepository, OrderStatusHistoryRepository historyRepository,
                        OrderStateService state, CatalogClient catalog, NotificationClient notifications,
                        @Value("${app.orders.reservation-ttl-minutes:60}") long reservationTtlMinutes) {
        this.orderRepository = orderRepository;
        this.historyRepository = historyRepository;
        this.state = state;
        this.catalog = catalog;
        this.notifications = notifications;
        this.reservationTtlMinutes = reservationTtlMinutes;
    }

    public OrderResponse create(RequestUser user, CreateOrderRequest request) {
        UUID orderId = UUID.randomUUID();
        CatalogClient.Reservation reservation = catalog.reserve(orderId,
                request.items().stream().map(i -> new CatalogClient.Item(i.productId(), i.quantity())).toList());
        try {
            Order order = new Order(orderId, user.id(), user.email());
            BigDecimal subtotal = BigDecimal.ZERO;
            for (CatalogClient.ReservedLine line : reservation.items()) {
                OrderItem item = new OrderItem();
                item.setProductId(line.productId());
                item.setProductName(line.brandName() == null || line.name().startsWith(line.brandName())
                        ? line.name() : line.brandName() + " " + line.name());
                item.setProductImage(line.imageUrl());
                item.setQuantity(line.quantity());
                item.setUnitPrice(line.unitPrice());
                item.setTotalPrice(line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())));
                order.addItem(item);
                subtotal = subtotal.add(item.getTotalPrice());
            }
            order.setSubtotal(subtotal);
            order.setTax(BigDecimal.ZERO);
            order.setTotal(subtotal);
            order.setCurrency(reservation.currency());
            order.setShippingAddress(request.shippingAddress().trim());
            order.setExpiresAt(LocalDateTime.now().plusMinutes(reservationTtlMinutes));
            Order saved = state.create(order);
            log.info("Order created: id={}, userId={}, total={} {}", saved.getId(), user.id(), saved.getTotal(), saved.getCurrency());
            notifications.send("ORDER_CREATED", saved, "We received your order " + shortId(saved),
                    "Thank you for your order. Total: " + saved.getTotal() + " " + saved.getCurrency()
                            + ". Your timepieces are reserved until payment is completed.");
            return toResponse(saved);
        } catch (RuntimeException e) {
            releaseQuietly(orderId);
            throw e;
        }
    }

    public List<OrderResponse> listOwn(RequestUser user) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.id()).stream().map(this::toResponse).toList();
    }

    public OrderResponse getForUser(UUID orderId, RequestUser user) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> user.isAdmin() || o.getUserId().equals(user.id()))
                .orElseThrow(() -> ApiException.notFound("Order"));
        return toResponse(order);
    }

    public OrderResponse getInternal(UUID orderId) {
        return toResponse(orderRepository.findById(orderId).orElseThrow(() -> ApiException.notFound("Order")));
    }

    public OrderResponse cancelOwn(UUID orderId, RequestUser user) {
        Order order = orderRepository.findById(orderId).filter(o -> o.getUserId().equals(user.id()))
                .orElseThrow(() -> ApiException.notFound("Order"));
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAYMENT_FAILED) {
            throw ApiException.conflict("CANNOT_CANCEL", "This order can no longer be cancelled");
        }
        return toResponse(cancel(orderId, "customer", "CUSTOMER_REQUEST"));
    }

    public Page<OrderResponse> adminList(String status, UUID userId, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("createdAt").descending());
        Page<Order> orders;
        if (userId != null) {
            orders = orderRepository.findByUserId(userId, pageable);
        } else if (status != null && !status.isBlank()) {
            orders = orderRepository.findByStatus(parseStatus(status), pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }
        return orders.map(this::toResponse);
    }

    /** Fulfilment steps only. Payment states are never set by a person: they come from the payment service. */
    public OrderResponse adminTransition(UUID orderId, String target, RequestUser admin) {
        OrderStatus status = parseStatus(target);
        if (!ADMIN_TARGETS.contains(status)) {
            throw ApiException.conflict("STATUS_NOT_ASSIGNABLE", "Status " + status + " is set by the payment process, not manually");
        }
        if (status == OrderStatus.CANCELLED) {
            return toResponse(cancel(orderId, admin.email(), "CANCELLED_BY_STORE"));
        }
        Change change = withRetry(() -> state.transition(orderId, status, admin.email(), null, null));
        if (change.changed()) {
            notifications.send("ORDER_" + status.name(), change.order(), "Your order " + shortId(change.order()) + " is " + label(status),
                    "Your order status changed to " + label(status) + ".");
        }
        return toResponse(change.order());
    }

    public Map<String, Object> stats() {
        Map<String, Object> byStatus = new LinkedHashMap<>();
        long total = 0;
        for (OrderStatus s : OrderStatus.values()) {
            long count = orderRepository.countByStatus(s);
            byStatus.put(s.name(), count);
            total += count;
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalOrders", total);
        stats.put("byStatus", byStatus);
        stats.put("revenue", orderRepository.sumTotalByStatusIn(PURCHASED));
        return stats;
    }

    public boolean hasPurchased(UUID userId, UUID productId) {
        return orderRepository.hasPurchased(userId, productId, PURCHASED);
    }

    /** Called by payment-service after it verified the payment with the provider. Idempotent. */
    public OrderResponse applyPaymentResult(UUID orderId, PaymentResult result, String paymentId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> ApiException.notFound("Order"));
        Consumer<Order> paymentInfo = o -> {
            o.setPaymentId(paymentId);
            o.setPaymentStatus(result.name());
        };
        switch (result) {
            case APPROVED -> {
                if (order.getStatus() == OrderStatus.CANCELLED) {
                    log.error("Payment {} approved for cancelled order {}: manual refund required", paymentId, orderId);
                    state.annotate(orderId, "Payment " + paymentId + " was approved after cancellation: refund required");
                    return toResponse(orderRepository.findById(orderId).orElseThrow());
                }
                if (order.getStatus().awaitingPayment()) {
                    Change change = withRetry(() -> state.transition(orderId, OrderStatus.PAID, "payment-service",
                            "Payment " + paymentId + " approved", paymentInfo.andThen(o -> o.setPaidAt(LocalDateTime.now()))));
                    if (change.changed()) {
                        if (!catalog.confirm(orderId)) {
                            log.error("Order {} was paid but its stock reservation was already released", orderId);
                            state.annotate(orderId, "Paid after the stock reservation was released: verify availability");
                        }
                        notifications.send("PAYMENT_APPROVED", change.order(), "Payment confirmed for order " + shortId(change.order()),
                                "We received your payment of " + change.order().getTotal() + " " + change.order().getCurrency()
                                        + ". We are preparing your order.");
                    }
                    return toResponse(change.order());
                }
            }
            case REJECTED -> {
                if (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.PAYMENT_PENDING) {
                    Change change = withRetry(() -> state.transition(orderId, OrderStatus.PAYMENT_FAILED, "payment-service",
                            "Payment " + paymentId + " rejected", paymentInfo));
                    if (change.changed()) {
                        notifications.send("PAYMENT_REJECTED", change.order(), "Payment not completed for order " + shortId(change.order()),
                                "Your payment was not approved. You can try again from your order page while the reservation is active.");
                    }
                    return toResponse(change.order());
                }
            }
            case PENDING -> {
                if (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.PAYMENT_FAILED) {
                    return toResponse(withRetry(() -> state.transition(orderId, OrderStatus.PAYMENT_PENDING, "payment-service",
                            "Payment " + paymentId + " in process", paymentInfo)).order());
                }
            }
            case REFUNDED -> {
                if (order.getStatus().paidOrLater()) {
                    Change change = withRetry(() -> state.transition(orderId, OrderStatus.REFUNDED, "payment-service",
                            "Payment " + paymentId + " refunded", paymentInfo));
                    if (change.changed()) {
                        releaseStock(orderId);
                        notifications.send("ORDER_REFUNDED", change.order(), "Refund issued for order " + shortId(change.order()),
                                "Your payment was refunded.");
                    }
                    return toResponse(change.order());
                }
            }
        }
        return toResponse(order);
    }

    public Order cancel(UUID orderId, String actor, String reason) {
        Change change = withRetry(() -> state.transition(orderId, OrderStatus.CANCELLED, actor, reason, o -> o.setCancelReason(reason)));
        if (change.changed()) {
            releaseStock(orderId);
            notifications.send("ORDER_CANCELLED", change.order(), "Order " + shortId(change.order()) + " cancelled",
                    "EXPIRED".equals(reason)
                            ? "Your order was cancelled because payment was not completed in time. The reserved items are available again."
                            : "Your order was cancelled.");
        }
        return change.order();
    }

    public void releaseStock(UUID orderId) {
        try {
            catalog.release(orderId);
            state.markStockReleased(orderId);
        } catch (RuntimeException e) {
            log.warn("Stock release for order {} postponed: {}", orderId, e.getMessage());
        }
    }

    private void releaseQuietly(UUID orderId) {
        try {
            catalog.release(orderId);
        } catch (RuntimeException e) {
            log.error("Could not release stock for failed order {}: {}", orderId, e.getMessage());
        }
    }

    private static Change withRetry(java.util.function.Supplier<Change> action) {
        OptimisticLockingFailureException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return action.get();
            } catch (OptimisticLockingFailureException e) {
                last = e;
            }
        }
        throw last;
    }

    private static OrderStatus parseStatus(String value) {
        try {
            return OrderStatus.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException e) {
            throw ApiException.badRequest("Unknown order status");
        }
    }

    private static String shortId(Order order) {
        return "#" + order.getId().toString().substring(0, 8).toUpperCase();
    }

    private static String label(OrderStatus status) {
        return status.name().toLowerCase().replace('_', ' ');
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.Item> items = order.getItems().stream()
                .map(i -> new OrderResponse.Item(i.getId(), i.getProductId(), i.getProductName(), i.getProductImage(),
                        i.getQuantity(), i.getUnitPrice(), i.getTotalPrice()))
                .toList();
        List<OrderResponse.StatusChange> history = historyRepository.findByOrderIdOrderByCreatedAtAscIdAsc(order.getId()).stream()
                .map(h -> new OrderResponse.StatusChange(h.getFromStatus() == null ? null : h.getFromStatus().name(),
                        h.getToStatus().name(), h.getActor(), h.getNote(), h.getCreatedAt()))
                .toList();
        List<String> next = order.getStatus().allowedNext().stream().filter(ADMIN_TARGETS::contains).map(Enum::name).toList();
        return new OrderResponse(order.getId(), order.getUserId(), order.getUserEmail(), order.getStatus().name(),
                order.getSubtotal(), order.getTax(), order.getTotal(), order.getCurrency(), order.getShippingAddress(),
                order.getPaymentId(), order.getPaymentStatus(), order.getCancelReason(), items, history, next,
                order.getExpiresAt(), order.getPaidAt(), order.getCreatedAt(), order.getUpdatedAt());
    }
}
