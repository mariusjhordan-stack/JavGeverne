package com.cronos.auth.repository;

import com.cronos.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Modifying
    @Query("update RefreshToken t set t.revoked = true where t.userId = :userId and t.revoked = false")
    int revokeAllForUser(@Param("userId") UUID userId);

    @Modifying
    @Query("update RefreshToken t set t.revoked = true where t.id = :id and t.revoked = false")
    int revoke(@Param("id") UUID id);

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :before")
    int deleteExpired(@Param("before") LocalDateTime before);
}
