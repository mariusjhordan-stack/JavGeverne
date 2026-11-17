package com.cronos.catalog.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/** Stock is deliberately absent: it only changes through reservations and stock adjustments. */
public record ProductUpdateRequest(
    @Size(min = 1, max = 255) String name,
    @Size(max = 5000) String description,
    @Size(max = 100) String referenceNumber,
    Long brandId,
    Long categoryId,
    @DecimalMin(value = "0.01", message = "Price must be greater than 0") @Digits(integer = 10, fraction = 2) BigDecimal price,
    @Size(max = 100) String caseMaterial,
    @Size(max = 50) String caseDiameter,
    @Pattern(regexp = "AUTOMATIC|MANUAL|QUARTZ", message = "Movement must be AUTOMATIC, MANUAL or QUARTZ") String movementType,
    @Size(max = 50) String waterResistance,
    @Size(max = 50) String crystalType,
    @Size(max = 50) String dialColor,
    @Size(max = 100) String strapMaterial,
    @Size(max = 50) String powerReserve,
    @Size(max = 500) String imageUrl,
    @Size(max = 12) List<@Size(max = 500) String> gallery,
    Boolean featured,
    Boolean active
) {}
