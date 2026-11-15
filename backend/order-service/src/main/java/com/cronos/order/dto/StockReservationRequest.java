package com.cronos.order.dto;

import java.util.UUID;

public record StockReservationRequest(
        UUID productId,
        int quantity
) {}
