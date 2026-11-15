package com.cronos.catalog.dto;

import java.math.BigDecimal;

public record ProductSearchCriteria(
    Long brandId,
    Long categoryId,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    String movementType,
    String query
) {}
