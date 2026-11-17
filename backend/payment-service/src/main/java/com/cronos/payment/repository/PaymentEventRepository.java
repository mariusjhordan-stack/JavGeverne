package com.cronos.payment.repository;

import com.cronos.payment.entity.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {

    boolean existsByProviderPaymentIdAndProviderStatus(String providerPaymentId, String providerStatus);

    List<PaymentEvent> findByPaymentIdOrderByCreatedAtAscIdAsc(UUID paymentId);
}
