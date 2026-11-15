package com.cronos.order.entity;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PAYMENT_PENDING,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED
}
