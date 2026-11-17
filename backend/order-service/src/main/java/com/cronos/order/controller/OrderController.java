package com.cronos.order.controller;

import com.cronos.order.dto.CreateOrderRequest;
import com.cronos.order.dto.OrderResponse;
import com.cronos.order.security.RequestUser;
import com.cronos.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@AuthenticationPrincipal RequestUser user, @Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(user, request);
    }

    @GetMapping
    public List<OrderResponse> list(@AuthenticationPrincipal RequestUser user) {
        return orderService.listOwn(user);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal RequestUser user, @PathVariable UUID id) {
        return orderService.getForUser(id, user);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal RequestUser user, @PathVariable UUID id) {
        return orderService.cancelOwn(id, user);
    }
}
