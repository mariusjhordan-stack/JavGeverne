package com.cronos.order.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateOrderStatusRequest(
        @NotBlank(message = "Status is required")
        String status,

        String paymentId,

        String paymentStatus
) {}
