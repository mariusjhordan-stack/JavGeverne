package com.cronos.catalog.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductUpdateRequest(
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    String description,

    @Size(max = 100, message = "Reference number must not exceed 100 characters")
    String referenceNumber,

    Long brandId,
    Long categoryId,

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    BigDecimal price,

    @Size(max = 3, message = "Currency code must be 3 characters")
    String currency,

    @Min(value = 0, message = "Stock must be non-negative")
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
    Boolean active
) {}
