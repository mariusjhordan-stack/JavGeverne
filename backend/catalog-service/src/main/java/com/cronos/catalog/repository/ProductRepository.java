package com.cronos.catalog.repository;

import com.cronos.catalog.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    @Override
    @EntityGraph(attributePaths = {"brand", "category"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"brand", "category"})
    Optional<Product> findWithBrandAndCategoryById(UUID id);

    @EntityGraph(attributePaths = {"brand", "category"})
    List<Product> findByFeaturedTrueAndActiveTrueOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"brand", "category"})
    List<Product> findByStockLessThanEqualAndActiveTrueOrderByStockAsc(int threshold);

    /** Atomic: the row is only updated while enough stock remains, so concurrent orders cannot oversell. */
    @Modifying(flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock - :quantity "
            + "where p.id = :id and p.active = true and :quantity > 0 and p.stock >= :quantity")
    int decrementStock(@Param("id") UUID id, @Param("quantity") int quantity);

    @Modifying(flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock + :delta where p.id = :id and p.stock + :delta >= 0")
    int adjustStock(@Param("id") UUID id, @Param("delta") int delta);

    @Query("select p.stock from Product p where p.id = :id")
    Integer currentStock(@Param("id") UUID id);

    long countByActiveTrue();

    @Query("select coalesce(sum(p.stock), 0) from Product p where p.active = true")
    long totalUnitsInStock();

    boolean existsByReferenceNumber(String referenceNumber);
}
