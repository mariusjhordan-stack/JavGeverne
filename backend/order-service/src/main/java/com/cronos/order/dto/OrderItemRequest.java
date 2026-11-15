package com.cronos.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemRequest(
        @NotNull(message = "Product ID is required")
        UUID productId,

        @NotBlank(message = "Product name is required")
        String productName,

        String productImage,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        @NotNull(message = "Unit price is required")
        @Positive(message = "Unit price must be positive")
        BigDecimal unitPrice
) {}
