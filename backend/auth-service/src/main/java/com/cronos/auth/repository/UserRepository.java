package com.cronos.auth.repository;

import com.cronos.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select u from User u where lower(u.email) like concat('%', :q, '%') "
            + "or lower(u.firstName) like concat('%', :q, '%') or lower(u.lastName) like concat('%', :q, '%')")
    Page<User> search(@Param("q") String q, Pageable pageable);
}
