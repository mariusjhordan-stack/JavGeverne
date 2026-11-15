package com.cronos.payment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreatePreferenceRequest(
        @NotNull UUID orderId,
        @NotEmpty @Valid List<PaymentItem> items,
        @Email String payerEmail,
        @NotNull @Positive BigDecimal totalAmount,
        @NotBlank String currency
) {}
