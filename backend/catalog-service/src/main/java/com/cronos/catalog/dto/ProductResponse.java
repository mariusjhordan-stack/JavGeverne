package com.cronos.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    String name,
    String description,
    String referenceNumber,
    Long brandId,
    String brandName,
    Long categoryId,
    String categoryName,
    BigDecimal price,
    String currency,
    Integer stock,
    String caseMaterial,
    String caseDiameter,
    String movementType,
    String waterResistance,
    String crystalType,
    String dialColor,
    String strapMaterial,
    String powerReserve,
    String imageUrl,
    String galleryUrls,
    Boolean featured,
    Boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
