package com.cronos.order.service;

import com.cronos.order.client.CatalogClient;
import com.cronos.order.dto.*;
import com.cronos.order.entity.Order;
import com.cronos.order.entity.OrderItem;
import com.cronos.order.entity.OrderStatus;
import com.cronos.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final CatalogClient catalogClient;

    public OrderService(OrderRepository orderRepository, CatalogClient catalogClient) {
        this.orderRepository = orderRepository;
        this.catalogClient = catalogClient;
    }

    @Transactional
    public OrderResponse createOrder(UUID userId, String userEmail, CreateOrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setUserEmail(userEmail);
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(request.shippingAddress());
        order.setCurrency("USD");

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> reservedItems = new ArrayList<>();

        for (OrderItemRequest itemReq : request.items()) {
            boolean reserved = catalogClient.reserveStock(itemReq.productId(), itemReq.quantity());
            if (!reserved) {
                // Roll back previously reserved stock
                for (OrderItem ri : reservedItems) {
                    catalogClient.releaseStock(ri.getProductId(), ri.getQuantity());
                }
                throw new IllegalStateException(
                        "Insufficient stock for product: " + itemReq.productName());
            }

            OrderItem item = new OrderItem();
            item.setProductId(itemReq.productId());
            item.setProductName(itemReq.productName());
            item.setProductImage(itemReq.productImage());
            item.setQuantity(itemReq.quantity());
            item.setUnitPrice(itemReq.unitPrice());
            item.setTotalPrice(itemReq.unitPrice().multiply(BigDecimal.valueOf(itemReq.quantity())));

            order.addItem(item);
            reservedItems.add(item);
            subtotal = subtotal.add(item.getTotalPrice());
        }

        order.setSubtotal(subtotal);
        order.setTax(BigDecimal.ZERO);
        order.setTotal(subtotal);

        Order saved = orderRepository.save(order);
        log.info("Order created: id={}, userId={}, total={}", saved.getId(), userId, saved.getTotal());
        return toResponse(saved);
    }

    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        return toResponse(order);
    }

    public OrderResponse getOrderByIdAndUserId(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        return toResponse(order);
    }

    public List<OrderResponse> getOrdersByUserId(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        OrderStatus newStatus = OrderStatus.valueOf(request.status());
        order.setStatus(newStatus);

        if (request.paymentId() != null) {
            order.setPaymentId(request.paymentId());
        }
        if (request.paymentStatus() != null) {
            order.setPaymentStatus(request.paymentStatus());
        }

        Order saved = orderRepository.save(order);
        log.info("Order status updated: id={}, status={}", orderId, newStatus);
        return toResponse(saved);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (userId != null && !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }

        if (order.getStatus() == OrderStatus.SHIPPED ||
            order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel order in status: " + order.getStatus());
        }

        if (order.getStatus() != OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                catalogClient.releaseStock(item.getProductId(), item.getQuantity());
            }
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.info("Order cancelled: id={}", orderId);
        }

        return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getProductName(),
                        item.getProductImage(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getTotalPrice()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getUserEmail(),
                order.getStatus().name(),
                order.getSubtotal(),
                order.getTax(),
                order.getTotal(),
                order.getCurrency(),
                order.getShippingAddress(),
                order.getPaymentId(),
                order.getPaymentStatus(),
                order.getNotes(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
