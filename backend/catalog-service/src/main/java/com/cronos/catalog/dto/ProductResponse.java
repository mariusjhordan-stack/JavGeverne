package com.cronos.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    String name,
    String description,
    String referenceNumber,
    Long brandId,
    String brandName,
    String brandSlug,
    Long categoryId,
    String categoryName,
    String categorySlug,
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
    List<String> gallery,
    Boolean featured,
    Boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
