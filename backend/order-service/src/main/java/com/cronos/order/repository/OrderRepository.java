package com.cronos.order.repository;

import com.cronos.order.entity.Order;
import com.cronos.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Page<Order> findByUserId(UUID userId, Pageable pageable);

    @Query("select o.id from Order o where o.status in :statuses and o.expiresAt < :now")
    List<UUID> findExpired(@Param("statuses") Collection<OrderStatus> statuses, @Param("now") LocalDateTime now);

    @Query("select o.id from Order o where o.status = :status and o.updatedAt < :before")
    List<UUID> findStale(@Param("status") OrderStatus status, @Param("before") LocalDateTime before);

    @Query("select o.id from Order o where o.stockReleasePending = true")
    List<UUID> findPendingStockRelease();

    long countByStatus(OrderStatus status);

    @Query("select coalesce(sum(o.total), 0) from Order o where o.status in :statuses")
    BigDecimal sumTotalByStatusIn(@Param("statuses") Collection<OrderStatus> statuses);

    @Query("select count(o) > 0 from Order o join o.items i where o.userId = :userId and i.productId = :productId and o.status in :statuses")
    boolean hasPurchased(@Param("userId") UUID userId, @Param("productId") UUID productId,
                         @Param("statuses") Collection<OrderStatus> statuses);
}
