package com.cronos.payment.repository;

import com.cronos.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findByExternalPaymentId(String externalPaymentId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByPreferenceId(String preferenceId);
}
