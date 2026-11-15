package com.cronos.catalog.controller;

import com.cronos.catalog.dto.*;
import com.cronos.catalog.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final CatalogService catalogService;

    public ProductController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String movementType) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        if (brandId != null || categoryId != null || minPrice != null || maxPrice != null || movementType != null) {
            ProductSearchCriteria criteria = new ProductSearchCriteria(brandId, categoryId, minPrice, maxPrice, movementType, null);
            return ResponseEntity.ok(catalogService.searchProducts(criteria, pageable));
        }
        return ResponseEntity.ok(catalogService.getAllProducts(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogService.getProductById(id));
    }

    @GetMapping("/featured")
    public ResponseEntity<List<ProductResponse>> getFeaturedProducts() {
        return ResponseEntity.ok(catalogService.getFeaturedProducts());
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size);
        ProductSearchCriteria criteria = new ProductSearchCriteria(null, null, null, null, null, q);
        return ResponseEntity.ok(catalogService.searchProducts(criteria, pageable));
    }

    @GetMapping("/brand/{brandId}")
    public ResponseEntity<Page<ProductResponse>> getProductsByBrand(
            @PathVariable Long brandId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(catalogService.getProductsByBrand(brandId, pageable));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ProductResponse>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(catalogService.getProductsByCategory(categoryId, pageable));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Valid @RequestBody ProductCreateRequest request) {
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createProduct(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable UUID id,
            @Valid @RequestBody ProductUpdateRequest request) {
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(catalogService.updateProduct(id, request));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<Map<String, Object>> updateStock(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        String action = (String) body.get("action");
        int quantity = (Integer) body.get("quantity");

        boolean success;
        if ("reserve".equals(action)) {
            success = catalogService.reserveStock(id, quantity);
        } else if ("release".equals(action)) {
            success = catalogService.releaseStock(id, quantity);
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid action. Use 'reserve' or 'release'"));
        }

        if (!success) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Insufficient stock"));
        }
        return ResponseEntity.ok(Map.of("success", true));
    }
}
