package com.cronos.catalog.stock;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Service-to-service only: never routed by the gateway and requires the internal token. */
@RestController
@RequestMapping("/internal/stock/reservations")
public class InternalStockController {

    public record ItemRequest(@NotNull UUID productId, @NotNull @Min(1) @Max(99) Integer quantity) {}

    public record ReserveRequest(@NotNull UUID orderId, @NotEmpty @Size(max = 50) List<@Valid ItemRequest> items) {}

    private final StockService stockService;

    public InternalStockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping
    public StockService.Reservation reserve(@Valid @RequestBody ReserveRequest request) {
        return stockService.reserve(request.orderId(),
                request.items().stream().map(i -> new StockService.Item(i.productId(), i.quantity())).toList());
    }

    @PostMapping("/{orderId}/release")
    public Map<String, Object> release(@PathVariable UUID orderId) {
        return Map.of("released", stockService.release(orderId));
    }

    @PostMapping("/{orderId}/confirm")
    public Map<String, Object> confirm(@PathVariable UUID orderId) {
        stockService.confirm(orderId);
        return Map.of("confirmed", true);
    }
}
