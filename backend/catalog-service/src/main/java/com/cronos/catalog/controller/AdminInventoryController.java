package com.cronos.catalog.controller;

import com.cronos.catalog.dto.ProductResponse;
import com.cronos.catalog.repository.ProductRepository;
import com.cronos.catalog.service.CatalogService;
import com.cronos.catalog.stock.StockMovement;
import com.cronos.catalog.stock.StockMovementRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/inventory")
public class AdminInventoryController {

    public record Summary(long totalProducts, long activeProducts, long unitsInStock, int lowStockThreshold,
                          List<ProductResponse> lowStock) {}

    public record Movement(Long id, UUID productId, int delta, int stockAfter, String reason, String reference,
                           String actor, LocalDateTime createdAt) {}

    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;
    private final CatalogService catalogService;
    private final int lowStockThreshold;

    public AdminInventoryController(ProductRepository productRepository, StockMovementRepository movementRepository,
                                    CatalogService catalogService,
                                    @Value("${app.inventory.low-stock-threshold:3}") int lowStockThreshold) {
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
        this.catalogService = catalogService;
        this.lowStockThreshold = lowStockThreshold;
    }

    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public Summary summary() {
        List<ProductResponse> low = productRepository.findByStockLessThanEqualAndActiveTrueOrderByStockAsc(lowStockThreshold)
                .stream().map(catalogService::toResponse).toList();
        return new Summary(productRepository.count(), productRepository.countByActiveTrue(),
                productRepository.totalUnitsInStock(), lowStockThreshold, low);
    }

    @GetMapping("/movements")
    public Page<Movement> movements(@RequestParam(required = false) UUID productId,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "30") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<StockMovement> result = productId == null
                ? movementRepository.findAllByOrderByCreatedAtDescIdDesc(pageable)
                : movementRepository.findByProductIdOrderByCreatedAtDescIdDesc(productId, pageable);
        return result.map(m -> new Movement(m.getId(), m.getProductId(), m.getDelta(), m.getStockAfter(),
                m.getReason().name(), m.getReference(), m.getActor(), m.getCreatedAt()));
    }
}
