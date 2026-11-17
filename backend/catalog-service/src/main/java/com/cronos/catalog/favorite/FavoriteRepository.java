package com.cronos.catalog.favorite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, Favorite.Key> {

    @Query("select f.productId from Favorite f where f.userId = :userId order by f.createdAt desc")
    List<UUID> findProductIds(@Param("userId") UUID userId);

    @Modifying
    @Query("delete from Favorite f where f.userId = :userId and f.productId = :productId")
    int remove(@Param("userId") UUID userId, @Param("productId") UUID productId);
}
