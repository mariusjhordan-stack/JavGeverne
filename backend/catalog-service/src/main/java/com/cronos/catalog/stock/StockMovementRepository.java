package com.cronos.catalog.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByProductIdOrderByCreatedAtDescIdDesc(UUID productId, Pageable pageable);

    Page<StockMovement> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
