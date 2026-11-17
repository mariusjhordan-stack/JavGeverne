package com.cronos.order.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 50)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 50)
    private OrderStatus toStatus;

    @Column(nullable = false)
    private String actor;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected OrderStatusHistory() {}

    public OrderStatusHistory(UUID orderId, OrderStatus fromStatus, OrderStatus toStatus, String actor, String note) {
        this.orderId = orderId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actor = actor;
        this.note = note;
        this.createdAt = LocalDateTime.now();
    }

    public OrderStatus getFromStatus() { return fromStatus; }
    public OrderStatus getToStatus() { return toStatus; }
    public String getActor() { return actor; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
