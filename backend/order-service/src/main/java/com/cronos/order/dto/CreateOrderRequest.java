package com.cronos.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** Only identifiers and quantities are accepted: names and prices always come from the catalog. */
public record CreateOrderRequest(
        @NotEmpty(message = "Order must contain at least one item") @Size(max = 50) List<@Valid Item> items,
        @NotBlank(message = "Shipping address is required") @Size(max = 1000) String shippingAddress
) {
    public record Item(
            @NotNull(message = "Product is required") UUID productId,
            @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1")
            @Max(value = 99, message = "Quantity is too large") Integer quantity
    ) {}
}
