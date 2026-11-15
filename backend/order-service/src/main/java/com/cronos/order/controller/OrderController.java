package com.cronos.order.controller;

import com.cronos.order.dto.CreateOrderRequest;
import com.cronos.order.dto.OrderResponse;
import com.cronos.order.dto.UpdateOrderStatusRequest;
import com.cronos.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Email") String userEmail,
            @Valid @RequestBody CreateOrderRequest request) {
        try {
            OrderResponse order = orderService.createOrder(UUID.fromString(userId), userEmail, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getUserOrders(
            @RequestHeader("X-User-Id") String userId) {
        List<OrderResponse> orders = orderService.getOrdersByUserId(UUID.fromString(userId));
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            OrderResponse order;
            if (userId != null) {
                order = orderService.getOrderByIdAndUserId(id, UUID.fromString(userId));
            } else {
                order = orderService.getOrderById(id);
            }
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        try {
            OrderResponse order = orderService.updateOrderStatus(id, request);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            UUID uid = userId != null ? UUID.fromString(userId) : null;
            OrderResponse order = orderService.cancelOrder(id, uid);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
