package com.cronos.catalog.dto;

public record BrandResponse(
    Long id,
    String name,
    String slug,
    String description,
    String story,
    String imageUrl,
    String country,
    long productCount
) {}
