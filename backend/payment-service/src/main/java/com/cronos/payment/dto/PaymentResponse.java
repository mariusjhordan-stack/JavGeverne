package com.cronos.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        String externalPaymentId,
        String preferenceId,
        String status,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String payerEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
