package com.cronos.catalog.controller;

import com.cronos.catalog.dto.ProductCreateRequest;
import com.cronos.catalog.dto.ProductResponse;
import com.cronos.catalog.dto.ProductUpdateRequest;
import com.cronos.catalog.security.RequestUser;
import com.cronos.catalog.service.CatalogService;
import com.cronos.catalog.service.CatalogService.ProductFilter;
import com.cronos.catalog.stock.StockService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    public record StockAdjustmentRequest(@NotNull @Min(-100000) @Max(100000) Integer delta, @Size(max = 255) String note) {}

    private final CatalogService catalogService;
    private final StockService stockService;

    public AdminProductController(CatalogService catalogService, StockService stockService) {
        this.catalogService = catalogService;
        this.stockService = stockService;
    }

    @GetMapping
    public Page<ProductResponse> list(@RequestParam(required = false) String q,
                                      @RequestParam(required = false) String sort,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "24") int size) {
        return catalogService.findProducts(new ProductFilter(q, null, null, null, null, null, null, true), sort, page, size);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable UUID id) {
        return catalogService.getProduct(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductCreateRequest request, @AuthenticationPrincipal RequestUser admin) {
        return catalogService.createProduct(request, admin.email());
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductUpdateRequest request) {
        return catalogService.updateProduct(id, request);
    }

    @PostMapping("/{id}/stock-adjustments")
    public Map<String, Object> adjustStock(@PathVariable UUID id, @Valid @RequestBody StockAdjustmentRequest request,
                                           @AuthenticationPrincipal RequestUser admin) {
        int stock = stockService.adjust(id, request.delta(), request.note(), admin.email());
        return Map.of("productId", id, "stock", stock);
    }
}
