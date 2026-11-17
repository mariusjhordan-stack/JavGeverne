package com.cronos.catalog.stock;

import com.cronos.catalog.entity.Product;
import com.cronos.catalog.repository.ProductRepository;
import com.cronos.catalog.security.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    public record Item(UUID productId, int quantity) {}

    public record ReservedLine(UUID productId, String name, String brandName, String imageUrl,
                               BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {}

    public record Reservation(UUID orderId, String currency, List<ReservedLine> items, BigDecimal total) {}

    private final ProductRepository productRepository;
    private final StockReservationRepository reservationRepository;
    private final StockMovementRepository movementRepository;

    public StockService(ProductRepository productRepository, StockReservationRepository reservationRepository,
                        StockMovementRepository movementRepository) {
        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
        this.movementRepository = movementRepository;
    }

    /**
     * All-or-nothing: any unavailable line rolls back the whole reservation. The returned prices are
     * the catalog's own and are what the order must be charged at. Repeating the call for the same
     * order returns the existing reservation instead of taking stock twice.
     */
    @Transactional
    public Reservation reserve(UUID orderId, List<Item> requested) {
        Map<UUID, Integer> quantities = new TreeMap<>();
        for (Item item : requested) {
            if (item.quantity() <= 0) {
                throw ApiException.badRequest("Quantity must be positive");
            }
            quantities.merge(item.productId(), item.quantity(), Integer::sum);
        }
        if (quantities.isEmpty()) {
            throw ApiException.badRequest("At least one item is required");
        }

        List<StockReservation> existing = reservationRepository.lockByOrderId(orderId);
        if (!existing.isEmpty()) {
            boolean same = existing.size() == quantities.size() && existing.stream().allMatch(r ->
                    r.getStatus() != StockReservation.Status.RELEASED
                            && Integer.valueOf(r.getQuantity()).equals(quantities.get(r.getProductId())));
            if (!same) {
                throw ApiException.conflict("RESERVATION_EXISTS", "This order already has a different stock reservation");
            }
            return describe(orderId, existing);
        }

        List<StockReservation> created = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : quantities.entrySet()) {
            UUID productId = entry.getKey();
            int quantity = entry.getValue();
            Product product = productRepository.findById(productId).filter(Product::getActive).orElseThrow(() ->
                    new ApiException(HttpStatus.CONFLICT, "PRODUCT_UNAVAILABLE", "A product in your order is no longer available"));
            if (productRepository.decrementStock(productId, quantity) == 0) {
                throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", "Insufficient stock for " + product.getName());
            }
            created.add(reservationRepository.save(new StockReservation(orderId, productId, quantity, product.getPrice())));
            movementRepository.save(new StockMovement(productId, -quantity, productRepository.currentStock(productId),
                    StockMovement.Reason.RESERVATION, "order:" + orderId, null));
        }
        log.info("Stock reserved for order {} ({} lines)", orderId, created.size());
        return describe(orderId, created);
    }

    /** Returns stock to the shelf. Safe to call any number of times. */
    @Transactional
    public int release(UUID orderId) {
        int released = 0;
        for (StockReservation reservation : reservationRepository.lockByOrderId(orderId)) {
            if (reservation.getStatus() == StockReservation.Status.RELEASED) {
                continue;
            }
            productRepository.adjustStock(reservation.getProductId(), reservation.getQuantity());
            movementRepository.save(new StockMovement(reservation.getProductId(), reservation.getQuantity(),
                    productRepository.currentStock(reservation.getProductId()),
                    StockMovement.Reason.RELEASE, "order:" + orderId, null));
            reservation.setStatus(StockReservation.Status.RELEASED);
            released++;
        }
        if (released > 0) {
            log.info("Stock released for order {} ({} lines)", orderId, released);
        }
        return released;
    }

    /** Marks reserved units as sold once the order is paid. Fails if they were already given back. */
    @Transactional
    public void confirm(UUID orderId) {
        List<StockReservation> reservations = reservationRepository.lockByOrderId(orderId);
        if (reservations.isEmpty()) {
            throw ApiException.notFound("Reservation");
        }
        if (reservations.stream().anyMatch(r -> r.getStatus() == StockReservation.Status.RELEASED)) {
            throw ApiException.conflict("RESERVATION_RELEASED", "The stock for this order was already released");
        }
        reservations.forEach(r -> r.setStatus(StockReservation.Status.CONFIRMED));
    }

    @Transactional
    public int adjust(UUID productId, int delta, String note, String actor) {
        if (delta == 0) {
            throw ApiException.badRequest("Adjustment must not be zero");
        }
        if (!productRepository.existsById(productId)) {
            throw ApiException.notFound("Product");
        }
        if (productRepository.adjustStock(productId, delta) == 0) {
            throw ApiException.conflict("INSUFFICIENT_STOCK", "Stock cannot go below zero");
        }
        int stockAfter = productRepository.currentStock(productId);
        movementRepository.save(new StockMovement(productId, delta, stockAfter, StockMovement.Reason.ADJUSTMENT, note, actor));
        return stockAfter;
    }

    private Reservation describe(UUID orderId, List<StockReservation> reservations) {
        List<ReservedLine> lines = new ArrayList<>();
        String currency = null;
        BigDecimal total = BigDecimal.ZERO;
        for (StockReservation r : reservations.stream().sorted(Comparator.comparing(StockReservation::getProductId)).toList()) {
            Product p = productRepository.findWithBrandAndCategoryById(r.getProductId()).orElseThrow();
            if (currency != null && !currency.equals(p.getCurrency())) {
                throw ApiException.conflict("MIXED_CURRENCY", "Products with different currencies cannot share an order");
            }
            currency = p.getCurrency();
            BigDecimal lineTotal = r.getUnitPrice().multiply(BigDecimal.valueOf(r.getQuantity()));
            total = total.add(lineTotal);
            lines.add(new ReservedLine(p.getId(), p.getName(), p.getBrand().getName(), p.getImageUrl(),
                    r.getUnitPrice(), r.getQuantity(), lineTotal));
        }
        return new Reservation(orderId, currency, lines, total);
    }
}
