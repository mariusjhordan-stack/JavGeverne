package com.cronos.order.controller;

import com.cronos.order.dto.OrderResponse;
import com.cronos.order.security.RequestUser;
import com.cronos.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    public record StatusRequest(@NotBlank String status) {}

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public Page<OrderResponse> list(@RequestParam(required = false) String status,
                                    @RequestParam(required = false) UUID userId,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return orderService.adminList(status, userId, page, size);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return orderService.stats();
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return orderService.getInternal(id);
    }

    @PutMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody StatusRequest request,
                                      @AuthenticationPrincipal RequestUser admin) {
        return orderService.adminTransition(id, request.status(), admin);
    }
}
