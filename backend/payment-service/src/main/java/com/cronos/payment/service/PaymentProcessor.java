package com.cronos.payment.service;

import com.cronos.payment.client.OrderClient;
import com.cronos.payment.entity.Payment;
import com.cronos.payment.entity.PaymentEvent;
import com.cronos.payment.entity.PaymentStatus;
import com.cronos.payment.repository.PaymentEventRepository;
import com.cronos.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Applies one provider payment, as reported by the provider's own API, to our records. The whole
 * step is one transaction: if the order service does not acknowledge, nothing is stored and the
 * provider's retry (or the reconciliation job) processes it again.
 */
@Service
public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);

    public enum Outcome { APPLIED, DUPLICATE, IGNORED }

    public record PaymentInfo(String id, String status, String statusDetail, BigDecimal transactionAmount,
                              String currencyId, String externalReference, String paymentMethodId, String payerEmail) {}

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository eventRepository;
    private final OrderClient orderClient;

    public PaymentProcessor(PaymentRepository paymentRepository, PaymentEventRepository eventRepository, OrderClient orderClient) {
        this.paymentRepository = paymentRepository;
        this.eventRepository = eventRepository;
        this.orderClient = orderClient;
    }

    @Transactional
    public Outcome apply(PaymentInfo info, String source) {
        UUID orderId;
        try {
            orderId = UUID.fromString(info.externalReference());
        } catch (RuntimeException e) {
            log.warn("Provider payment {} has no usable external reference", info.id());
            return Outcome.IGNORED;
        }
        // The row lock serialises concurrent notifications for the same order.
        Optional<Payment> found = paymentRepository.lockByOrderId(orderId);
        if (found.isEmpty()) {
            log.warn("Provider payment {} references order {} for which no checkout was started here", info.id(), orderId);
            return Outcome.IGNORED;
        }
        Payment payment = found.get();
        if (info.transactionAmount() == null || payment.getAmount().compareTo(info.transactionAmount()) != 0) {
            log.error("Amount mismatch on provider payment {} for order {}: expected {}, provider says {}",
                    info.id(), orderId, payment.getAmount(), info.transactionAmount());
            return Outcome.IGNORED;
        }
        if (info.currencyId() == null || !payment.getCurrency().equalsIgnoreCase(info.currencyId())) {
            log.error("Currency mismatch on provider payment {} for order {}: expected {}, provider says {}",
                    info.id(), orderId, payment.getCurrency(), info.currencyId());
            return Outcome.IGNORED;
        }
        String providerStatus = info.status() == null ? "unknown" : info.status().toLowerCase();
        if (eventRepository.existsByProviderPaymentIdAndProviderStatus(info.id(), providerStatus)) {
            return Outcome.DUPLICATE;
        }
        eventRepository.saveAndFlush(new PaymentEvent(payment.getId(), info.id(), providerStatus, info.statusDetail(),
                info.transactionAmount(), info.currencyId(), source));

        PaymentStatus incoming = mapStatus(providerStatus);
        boolean sameProviderPayment = info.id().equals(payment.getExternalPaymentId());
        boolean reversal = incoming == PaymentStatus.REFUNDED;

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return Outcome.IGNORED;
        }
        if (payment.getStatus() == PaymentStatus.APPROVED && !(reversal && sameProviderPayment)) {
            // An approved payment is only undone by a refund of that same payment, never by another attempt.
            return Outcome.IGNORED;
        }
        if (incoming == null) {
            return Outcome.IGNORED;
        }
        if (sameProviderPayment && (payment.getStatus() == incoming
                || ((payment.getStatus() == PaymentStatus.REJECTED || payment.getStatus() == PaymentStatus.CANCELLED)
                && incoming == PaymentStatus.IN_PROCESS))) {
            return Outcome.IGNORED;
        }

        payment.setStatus(incoming);
        payment.setStatusDetail(info.statusDetail());
        payment.setExternalPaymentId(info.id());
        payment.setPaymentMethod(info.paymentMethodId());
        payment.setPayerEmail(info.payerEmail());
        payment.setWebhookReceivedAt(LocalDateTime.now());
        if (incoming == PaymentStatus.APPROVED) {
            payment.setVerifiedAt(LocalDateTime.now());
        }
        paymentRepository.saveAndFlush(payment);

        String orderResult = switch (incoming) {
            case APPROVED -> "APPROVED";
            case REJECTED, CANCELLED -> "REJECTED";
            case IN_PROCESS, PENDING -> "PENDING";
            case REFUNDED -> "REFUNDED";
        };
        orderClient.reportPaymentResult(orderId, orderResult, info.id());
        log.info("Provider payment {} ({}) applied to order {} as {}", info.id(), providerStatus, orderId, incoming);
        return Outcome.APPLIED;
    }

    static PaymentStatus mapStatus(String providerStatus) {
        return switch (providerStatus) {
            case "succeeded" -> PaymentStatus.APPROVED;
            case "failed" -> PaymentStatus.REJECTED;
            case "canceled" -> PaymentStatus.CANCELLED;
            case "expired" -> PaymentStatus.REJECTED;
            case "refunded" -> PaymentStatus.REFUNDED;
            case "pending", "processing", "unpaid" -> PaymentStatus.IN_PROCESS;
            default -> null;
        };
    }
}
