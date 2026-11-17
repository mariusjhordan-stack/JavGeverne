package com.cronos.catalog.favorite;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "favorites")
@IdClass(Favorite.Key.class)
public class Favorite {

    public record Key(UUID userId, UUID productId) implements Serializable {}

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Favorite() {}

    public Favorite(UUID userId, UUID productId) {
        this.userId = userId;
        this.productId = productId;
    }

    public UUID getUserId() { return userId; }
    public UUID getProductId() { return productId; }
}
