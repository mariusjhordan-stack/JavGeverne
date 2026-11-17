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
        String cancelReason,
        List<Item> items,
        List<StatusChange> history,
        List<String> allowedNextStatuses,
        LocalDateTime expiresAt,
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record Item(UUID id, UUID productId, String productName, String productImage, int quantity,
                       BigDecimal unitPrice, BigDecimal totalPrice) {}

    public record StatusChange(String from, String to, String actor, String note, LocalDateTime at) {}
}
