package com.cronos.payment.service;

import com.cronos.payment.config.StripeProperties;
import com.cronos.payment.entity.PaymentStatus;
import com.cronos.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.UUID;

/** Safety net for notifications that never arrived: asks the provider about checkouts still open. */
@Component
@ConditionalOnProperty(name = "stripe.reconcile-enabled", havingValue = "true", matchIfMissing = true)
public class PaymentReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationJob.class);

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final StripeProperties properties;

    public PaymentReconciliationJob(PaymentRepository paymentRepository, PaymentService paymentService,
                                    StripeProperties properties) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${stripe.reconcile-interval-ms:300000}", initialDelayString = "${stripe.reconcile-interval-ms:300000}")
    public void run() {
        if (!properties.isConfigured()) {
            return;
        }
        for (UUID orderId : paymentRepository.findOrderIdsToReconcile(
                EnumSet.of(PaymentStatus.PENDING, PaymentStatus.IN_PROCESS, PaymentStatus.REJECTED), LocalDateTime.now().minusDays(7))) {
            try {
                paymentService.reconcile(orderId);
            } catch (RuntimeException e) {
                log.warn("Reconciliation of order {} failed: {}", orderId, e.getMessage());
            }
        }
    }
}
