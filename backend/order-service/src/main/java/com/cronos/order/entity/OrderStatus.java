package com.cronos.order.entity;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {
    PENDING,
    PAYMENT_PENDING,
    PAYMENT_FAILED,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED;

    public Set<OrderStatus> allowedNext() {
        return switch (this) {
            case PENDING -> EnumSet.of(PAYMENT_PENDING, PAYMENT_FAILED, PAID, CANCELLED);
            case PAYMENT_PENDING -> EnumSet.of(PAYMENT_FAILED, PAID, CANCELLED);
            case PAYMENT_FAILED -> EnumSet.of(PAYMENT_PENDING, PAID, CANCELLED);
            case PAID -> EnumSet.of(PROCESSING, REFUNDED);
            case PROCESSING -> EnumSet.of(SHIPPED, REFUNDED);
            case SHIPPED -> EnumSet.of(DELIVERED, REFUNDED);
            case DELIVERED -> EnumSet.of(REFUNDED);
            case CANCELLED, REFUNDED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean awaitingPayment() {
        return this == PENDING || this == PAYMENT_PENDING || this == PAYMENT_FAILED;
    }

    public boolean paidOrLater() {
        return this == PAID || this == PROCESSING || this == SHIPPED || this == DELIVERED;
    }
}
