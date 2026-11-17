package com.cronos.notification.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    public enum Type {
        ORDER_CREATED, PAYMENT_APPROVED, PAYMENT_REJECTED, ORDER_CANCELLED, ORDER_REFUNDED,
        ORDER_PROCESSING, ORDER_SHIPPED, ORDER_DELIVERED
    }

    /** SENT = accepted by the SMTP server; SKIPPED = e-mail is not configured; FAILED = the server refused or was unreachable. */
    public enum Status { PENDING, SENT, SKIPPED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Type type;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Status status = Status.PENDING;

    @Column(length = 500)
    private String error;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected Notification() {}

    public Notification(UUID userId, String userEmail, Type type, String subject, String body, UUID orderId) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.type = type;
        this.subject = subject;
        this.body = body;
        this.orderId = orderId;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getUserEmail() { return userEmail; }
    public Type getType() { return type; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public UUID getOrderId() { return orderId; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
