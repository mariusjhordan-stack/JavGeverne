package com.cronos.catalog.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    public enum MovementType {
        AUTOMATIC, MANUAL, QUARTZ
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "reference_number", unique = true, length = 100)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "case_material", length = 100)
    private String caseMaterial;

    @Column(name = "case_diameter", length = 50)
    private String caseDiameter;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType = MovementType.AUTOMATIC;

    @Column(name = "water_resistance", length = 50)
    private String waterResistance;

    @Column(name = "crystal_type", length = 50)
    private String crystalType;

    @Column(name = "dial_color", length = 50)
    private String dialColor;

    @Column(name = "strap_material", length = 100)
    private String strapMaterial;

    @Column(name = "power_reserve", length = 50)
    private String powerReserve;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "gallery_urls", columnDefinition = "TEXT")
    private String galleryUrls;

    @Column(nullable = false)
    private Boolean featured = false;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public Brand getBrand() { return brand; }
    public void setBrand(Brand brand) { this.brand = brand; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getCaseMaterial() { return caseMaterial; }
    public void setCaseMaterial(String caseMaterial) { this.caseMaterial = caseMaterial; }
    public String getCaseDiameter() { return caseDiameter; }
    public void setCaseDiameter(String caseDiameter) { this.caseDiameter = caseDiameter; }
    public MovementType getMovementType() { return movementType; }
    public void setMovementType(MovementType movementType) { this.movementType = movementType; }
    public String getWaterResistance() { return waterResistance; }
    public void setWaterResistance(String waterResistance) { this.waterResistance = waterResistance; }
    public String getCrystalType() { return crystalType; }
    public void setCrystalType(String crystalType) { this.crystalType = crystalType; }
    public String getDialColor() { return dialColor; }
    public void setDialColor(String dialColor) { this.dialColor = dialColor; }
    public String getStrapMaterial() { return strapMaterial; }
    public void setStrapMaterial(String strapMaterial) { this.strapMaterial = strapMaterial; }
    public String getPowerReserve() { return powerReserve; }
    public void setPowerReserve(String powerReserve) { this.powerReserve = powerReserve; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getGalleryUrls() { return galleryUrls; }
    public void setGalleryUrls(String galleryUrls) { this.galleryUrls = galleryUrls; }
    public Boolean getFeatured() { return featured; }
    public void setFeatured(Boolean featured) { this.featured = featured; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
