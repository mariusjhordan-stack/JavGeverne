package com.cronos.catalog.stock;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_movements")
public class StockMovement {

    public enum Reason { INITIAL, RESERVATION, RELEASE, ADJUSTMENT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int delta;

    @Column(name = "stock_after", nullable = false)
    private int stockAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Reason reason;

    @Column(length = 255)
    private String reference;

    @Column(length = 255)
    private String actor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected StockMovement() {}

    public StockMovement(UUID productId, int delta, int stockAfter, Reason reason, String reference, String actor) {
        this.productId = productId;
        this.delta = delta;
        this.stockAfter = stockAfter;
        this.reason = reason;
        this.reference = reference;
        this.actor = actor;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public UUID getProductId() { return productId; }
    public int getDelta() { return delta; }
    public int getStockAfter() { return stockAfter; }
    public Reason getReason() { return reason; }
    public String getReference() { return reference; }
    public String getActor() { return actor; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
