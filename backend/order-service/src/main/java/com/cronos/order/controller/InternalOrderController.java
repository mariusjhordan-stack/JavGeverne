package com.cronos.order.controller;

import com.cronos.order.dto.OrderResponse;
import com.cronos.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/** Service-to-service only: never routed by the gateway and requires the internal token. */
@RestController
@RequestMapping("/internal/orders")
public class InternalOrderController {

    public record PaymentResultRequest(@NotNull OrderService.PaymentResult result, @NotBlank String paymentId) {}

    private final OrderService orderService;

    public InternalOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return orderService.getInternal(id);
    }

    @PostMapping("/{id}/payment-result")
    public OrderResponse paymentResult(@PathVariable UUID id, @Valid @RequestBody PaymentResultRequest request) {
        return orderService.applyPaymentResult(id, request.result(), request.paymentId());
    }

    @GetMapping("/purchased")
    public Map<String, Object> purchased(@RequestParam UUID userId, @RequestParam UUID productId) {
        return Map.of("purchased", orderService.hasPurchased(userId, productId));
    }
}
