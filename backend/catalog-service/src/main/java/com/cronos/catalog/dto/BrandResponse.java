package com.cronos.catalog.dto;

import java.time.LocalDateTime;

public record BrandResponse(
    Long id,
    String name,
    String description,
    String logoUrl,
    String country,
    LocalDateTime createdAt
) {}
