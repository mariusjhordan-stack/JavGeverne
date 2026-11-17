package com.cronos.catalog.controller;

import com.cronos.catalog.dto.ProductResponse;
import com.cronos.catalog.service.CatalogService;
import com.cronos.catalog.service.CatalogService.ProductFilter;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final CatalogService catalogService;

    public ProductController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public Page<ProductResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String movementType,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return catalogService.findProducts(
                new ProductFilter(q, brandId, categoryId, minPrice, maxPrice, movementType, featured, false), sort, page, size);
    }

    @GetMapping("/featured")
    public List<ProductResponse> featured() {
        return catalogService.getFeaturedProducts();
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable UUID id) {
        return catalogService.getActiveProduct(id);
    }
}
