package com.cronos.payment.service;

import com.cronos.payment.client.MercadoPagoClient;
import com.cronos.payment.client.OrderClient;
import com.cronos.payment.config.MercadoPagoConfig;
import com.cronos.payment.dto.*;
import com.cronos.payment.entity.Payment;
import com.cronos.payment.entity.PaymentStatus;
import com.cronos.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final MercadoPagoClient mercadoPagoClient;
    private final MercadoPagoConfig mercadoPagoConfig;
    private final OrderClient orderClient;

    public PaymentService(
            PaymentRepository paymentRepository,
            MercadoPagoClient mercadoPagoClient,
            MercadoPagoConfig mercadoPagoConfig,
            OrderClient orderClient) {
        this.paymentRepository = paymentRepository;
        this.mercadoPagoClient = mercadoPagoClient;
        this.mercadoPagoConfig = mercadoPagoConfig;
        this.orderClient = orderClient;
    }

    @Transactional
    public PreferenceResponse createPreference(CreatePreferenceRequest request) {
        List<Map<String, Object>> mpItems = request.items().stream()
                .map(item -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("title", item.title());
                    m.put("quantity", item.quantity());
                    m.put("unit_price", item.unitPrice());
                    m.put("currency_id", request.currency());
                    return m;
                })
                .toList();

        String externalReference = request.orderId().toString();

        Optional<MercadoPagoClient.PreferenceResult> result = mercadoPagoClient.createPreference(
                externalReference,
                mpItems,
                request.payerEmail(),
                mercadoPagoConfig.getNotificationUrl()
        );

        Payment payment = new Payment();
        payment.setOrderId(request.orderId());
        payment.setAmount(request.totalAmount());
        payment.setCurrency(request.currency());
        payment.setPayerEmail(request.payerEmail());
        payment.setStatus(PaymentStatus.PENDING);

        if (result.isPresent()) {
            MercadoPagoClient.PreferenceResult pref = result.get();
            payment.setPreferenceId(pref.preferenceId());
            paymentRepository.save(payment);

            return new PreferenceResponse(
                    pref.preferenceId(),
                    pref.initPoint(),
                    pref.sandboxInitPoint()
            );
        }

        paymentRepository.save(payment);
        log.warn("Mercado Pago preference not created (not configured). Payment record saved for order {}", request.orderId());
        return new PreferenceResponse(null, null, null);
    }

    @Transactional
    public void processWebhook(WebhookPayload payload, String xSignature, String xRequestId) {
        if (payload == null || payload.data() == null || payload.data().id() == null) {
            log.warn("Received webhook with missing data");
            return;
        }

        if (!"payment".equals(payload.type())) {
            log.info("Ignoring non-payment webhook type: {}", payload.type());
            return;
        }

        String paymentId = payload.data().id();
        String idempotencyKey = "webhook:" + paymentId;

        if (paymentRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Duplicate webhook for payment {} - already processed", paymentId);
            return;
        }

        validateWebhookSignature(paymentId, xSignature, xRequestId);

        Optional<MercadoPagoClient.PaymentInfo> paymentInfoOpt = mercadoPagoClient.getPayment(paymentId);
        if (paymentInfoOpt.isEmpty()) {
            log.error("Could not retrieve payment {} from Mercado Pago", paymentId);
            return;
        }

        MercadoPagoClient.PaymentInfo mpPayment = paymentInfoOpt.get();
        String externalReference = mpPayment.externalReference();

        UUID orderId;
        try {
            orderId = UUID.fromString(externalReference);
        } catch (IllegalArgumentException e) {
            log.error("Invalid external reference (not a UUID): {}", externalReference);
            return;
        }

        List<Payment> existingPayments = paymentRepository.findByOrderId(orderId);
        Payment payment;
        if (!existingPayments.isEmpty()) {
            payment = existingPayments.getFirst();
            if (payment.getAmount().compareTo(mpPayment.transactionAmount()) != 0) {
                log.error("Amount mismatch for order {}: expected {}, got {}",
                        orderId, payment.getAmount(), mpPayment.transactionAmount());
                return;
            }
            if (!payment.getCurrency().equalsIgnoreCase(mpPayment.currencyId())) {
                log.error("Currency mismatch for order {}: expected {}, got {}",
                        orderId, payment.getCurrency(), mpPayment.currencyId());
                return;
            }
        } else {
            payment = new Payment();
            payment.setOrderId(orderId);
            payment.setAmount(mpPayment.transactionAmount());
            payment.setCurrency(mpPayment.currencyId());
        }

        payment.setExternalPaymentId(paymentId);
        payment.setPaymentMethod(mpPayment.paymentMethodId());
        payment.setPayerEmail(mpPayment.payerEmail());
        payment.setIdempotencyKey(idempotencyKey);
        payment.setWebhookReceivedAt(LocalDateTime.now());

        PaymentStatus newStatus = mapMercadoPagoStatus(mpPayment.status());
        payment.setStatus(newStatus);

        if (newStatus == PaymentStatus.APPROVED) {
            payment.setVerifiedAt(LocalDateTime.now());
        }

        paymentRepository.save(payment);
        log.info("Payment {} for order {} updated to status {}", paymentId, orderId, newStatus);

        String orderStatus = switch (newStatus) {
            case APPROVED -> "PAID";
            case REJECTED -> "PAYMENT_FAILED";
            case CANCELLED -> "CANCELLED";
            case IN_PROCESS -> "PAYMENT_PENDING";
            default -> null;
        };

        if (orderStatus != null) {
            orderClient.updateOrderStatus(orderId, orderStatus);
        }
    }

    public void validateWebhookSignature(String dataId, String xSignature, String xRequestId) {
        if (mercadoPagoConfig.getWebhookSecret() == null || mercadoPagoConfig.getWebhookSecret().isBlank()) {
            log.warn("Webhook secret not configured - signature validation skipped");
            return;
        }

        if (xSignature == null || xSignature.isBlank()) {
            throw new SecurityException("Missing x-signature header");
        }

        String ts = null;
        String v1 = null;
        for (String part : xSignature.split(",")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("ts=")) {
                ts = trimmed.substring(3);
            } else if (trimmed.startsWith("v1=")) {
                v1 = trimmed.substring(3);
            }
        }

        if (ts == null || v1 == null) {
            throw new SecurityException("Invalid x-signature format: missing ts or v1");
        }

        String manifest = String.format("id:%s;request-id:%s;ts:%s;",
                dataId,
                xRequestId != null ? xRequestId : "",
                ts);

        String computedHmac = computeHmacSha256(manifest, mercadoPagoConfig.getWebhookSecret());

        if (!v1.equals(computedHmac)) {
            throw new SecurityException("Webhook signature mismatch");
        }

        log.debug("Webhook signature validated for payment {}", dataId);
    }

    private String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }

    private PaymentStatus mapMercadoPagoStatus(String mpStatus) {
        if (mpStatus == null) return PaymentStatus.PENDING;
        return switch (mpStatus.toLowerCase()) {
            case "approved" -> PaymentStatus.APPROVED;
            case "rejected" -> PaymentStatus.REJECTED;
            case "cancelled" -> PaymentStatus.CANCELLED;
            case "refunded" -> PaymentStatus.REFUNDED;
            case "in_process", "in_mediation", "pending" -> PaymentStatus.IN_PROCESS;
            default -> PaymentStatus.PENDING;
        };
    }

    public List<PaymentResponse> getPaymentsByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .map(this::toResponse)
                .toList();
    }

    public Optional<PaymentResponse> getPaymentById(UUID id) {
        return paymentRepository.findById(id).map(this::toResponse);
    }

    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getOrderId(),
                p.getExternalPaymentId(),
                p.getPreferenceId(),
                p.getStatus().name(),
                p.getAmount(),
                p.getCurrency(),
                p.getPaymentMethod(),
                p.getPayerEmail(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
