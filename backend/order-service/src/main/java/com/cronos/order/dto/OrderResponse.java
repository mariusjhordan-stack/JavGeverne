package com.cronos.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        String userEmail,
        String status,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal total,
        String currency,
        String shippingAddress,
        String paymentId,
        String paymentStatus,
        String notes,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
