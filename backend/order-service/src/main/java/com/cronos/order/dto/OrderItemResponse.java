package com.cronos.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        String productName,
        String productImage,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {}
