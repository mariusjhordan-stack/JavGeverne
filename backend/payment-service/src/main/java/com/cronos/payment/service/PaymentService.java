package com.cronos.payment.service;

import com.cronos.payment.client.MercadoPagoClient;
import com.cronos.payment.client.MercadoPagoClient.PaymentInfo;
import com.cronos.payment.client.OrderClient;
import com.cronos.payment.client.OrderClient.OrderSnapshot;
import com.cronos.payment.config.MercadoPagoProperties;
import com.cronos.payment.entity.Payment;
import com.cronos.payment.entity.PaymentStatus;
import com.cronos.payment.repository.PaymentEventRepository;
import com.cronos.payment.repository.PaymentRepository;
import com.cronos.payment.security.ApiException;
import com.cronos.payment.security.RequestUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final Set<String> PAYABLE = Set.of("PENDING", "PAYMENT_FAILED");
    private static final DateTimeFormatter MP_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    public record CheckoutResponse(UUID orderId, String preferenceId, String checkoutUrl) {}

    public record PaymentView(UUID id, UUID orderId, UUID userId, String status, String statusDetail, BigDecimal amount,
                              String currency, String externalPaymentId, String preferenceId, String paymentMethod,
                              String payerEmail, LocalDateTime verifiedAt, LocalDateTime createdAt, LocalDateTime updatedAt,
                              List<EventView> events) {}

    public record EventView(String providerPaymentId, String providerStatus, String source, LocalDateTime at) {}

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository eventRepository;
    private final PaymentProcessor processor;
    private final MercadoPagoClient mercadoPago;
    private final OrderClient orderClient;
    private final MercadoPagoProperties properties;
    private final WebhookSignatureVerifier signatureVerifier;

    public PaymentService(PaymentRepository paymentRepository, PaymentEventRepository eventRepository,
                          PaymentProcessor processor, MercadoPagoClient mercadoPago, OrderClient orderClient,
                          MercadoPagoProperties properties, WebhookSignatureVerifier signatureVerifier) {
        this.paymentRepository = paymentRepository;
        this.eventRepository = eventRepository;
        this.processor = processor;
        this.mercadoPago = mercadoPago;
        this.orderClient = orderClient;
        this.properties = properties;
        this.signatureVerifier = signatureVerifier;
    }

    /** Starts a Checkout Pro session. Amount, currency and items are read from the order, never from the client. */
    public CheckoutResponse startCheckout(UUID orderId, RequestUser user) {
        if (!properties.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_PROVIDER_NOT_CONFIGURED",
                    "Online payment is not available right now. Your order is saved and you can pay later");
        }
        OrderSnapshot order = orderClient.getOrder(orderId)
                .filter(o -> o.userId().equals(user.id()))
                .orElseThrow(() -> ApiException.notFound("Order"));
        if (!PAYABLE.contains(order.status())) {
            throw ApiException.conflict("ORDER_NOT_PAYABLE", "PAYMENT_PENDING".equals(order.status())
                    ? "A payment for this order is already being processed"
                    : "This order cannot be paid in its current status");
        }
        if (order.total() == null || order.total().signum() <= 0 || order.items() == null || order.items().isEmpty()) {
            throw ApiException.conflict("ORDER_NOT_PAYABLE", "This order has nothing to pay");
        }

        Payment payment = paymentRepository.findByOrderId(orderId).orElseGet(() -> new Payment(orderId, order.userId()));
        if (payment.getStatus() == PaymentStatus.APPROVED || payment.getStatus() == PaymentStatus.REFUNDED) {
            throw ApiException.conflict("ORDER_NOT_PAYABLE", "This order was already paid");
        }

        MercadoPagoClient.PreferenceResult preference = mercadoPago.createPreference(buildPreference(order), UUID.randomUUID().toString());
        String checkoutUrl = properties.accessToken().startsWith("TEST-") && preference.sandboxInitPoint() != null
                ? preference.sandboxInitPoint() : preference.initPoint();

        payment.setAmount(order.total());
        payment.setCurrency(order.currency());
        payment.setPayerEmail(order.userEmail());
        payment.setPreferenceId(preference.preferenceId());
        payment.setCheckoutUrl(checkoutUrl);
        try {
            paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict("CHECKOUT_IN_PROGRESS", "A checkout for this order was just started. Try again");
        }
        return new CheckoutResponse(orderId, preference.preferenceId(), checkoutUrl);
    }

    private Map<String, Object> buildPreference(OrderSnapshot order) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderClient.OrderItem item : order.items()) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("title", item.productName());
            line.put("quantity", item.quantity());
            line.put("unit_price", item.unitPrice());
            line.put("currency_id", order.currency());
            items.add(line);
        }
        String query = "?order=" + order.id();
        String success = properties.returnUrl("success") + query;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("external_reference", order.id().toString());
        body.put("payer", Map.of("email", order.userEmail()));
        body.put("back_urls", Map.of(
                "success", success,
                "failure", properties.returnUrl("failure") + query,
                "pending", properties.returnUrl("pending") + query));
        if (success.startsWith("https://")) {
            body.put("auto_return", "approved");
        }
        if (properties.webhookUrl() != null && !properties.webhookUrl().isBlank()) {
            body.put("notification_url", properties.webhookUrl());
        }
        if (order.expiresAt() != null && order.expiresAt().isAfter(LocalDateTime.now())) {
            // The provider stops accepting payment when our stock reservation ends.
            body.put("expires", true);
            body.put("expiration_date_to", order.expiresAt().atZone(ZoneId.systemDefault()).format(MP_DATE));
        }
        body.put("statement_descriptor", "CRONOS GENEVE");
        return body;
    }

    /** Signature first, then the payment is fetched from the provider: the notification body is never trusted. */
    public void handleWebhook(String queryDataId, String type, String bodyDataId, String signature, String requestId) {
        String dataId = queryDataId != null && !queryDataId.isBlank() ? queryDataId : bodyDataId;
        signatureVerifier.verify(dataId, signature, requestId);
        if (type != null && !"payment".equals(type)) {
            return;
        }
        if (!properties.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_PROVIDER_NOT_CONFIGURED", "Payment provider is not configured");
        }
        mercadoPago.getPayment(dataId).ifPresentOrElse(
                info -> applySafely(info, "webhook"),
                () -> log.warn("Webhook for payment {} that the provider does not know", dataId));
    }

    /** Asks the provider what happened with an order. Used by the return pages and by reconciliation. */
    public PaymentView sync(UUID orderId, RequestUser user) {
        Payment payment = owned(orderId, user);
        if (properties.isConfigured() && payment.getPreferenceId() != null) {
            reconcile(orderId);
        }
        return toView(paymentRepository.findByOrderId(orderId).orElse(payment));
    }

    public void reconcile(UUID orderId) {
        for (PaymentInfo info : mercadoPago.findByExternalReference(orderId.toString())) {
            applySafely(info, "sync");
        }
    }

    private void applySafely(PaymentInfo info, String source) {
        try {
            processor.apply(info, source);
        } catch (DataIntegrityViolationException e) {
            // Another request stored the same provider event first.
            log.info("Provider payment {} was processed concurrently", info.id());
        }
    }

    public PaymentView getForOrder(UUID orderId, RequestUser user) {
        return toView(owned(orderId, user));
    }

    private Payment owned(UUID orderId, RequestUser user) {
        return paymentRepository.findByOrderId(orderId)
                .filter(p -> user.isAdmin() || user.id().equals(p.getUserId()))
                .orElseThrow(() -> ApiException.notFound("Payment"));
    }

    public Page<PaymentView> adminList(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("createdAt").descending());
        if (status == null || status.isBlank()) {
            return paymentRepository.findAll(pageable).map(this::toView);
        }
        try {
            return paymentRepository.findByStatus(PaymentStatus.valueOf(status.trim().toUpperCase()), pageable).map(this::toView);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Unknown payment status");
        }
    }

    public Map<String, Object> stats() {
        Map<String, Object> byStatus = new LinkedHashMap<>();
        for (PaymentStatus s : PaymentStatus.values()) {
            byStatus.put(s.name(), paymentRepository.countByStatus(s));
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("byStatus", byStatus);
        stats.put("approvedAmount", paymentRepository.sumAmountByStatus(PaymentStatus.APPROVED));
        stats.put("providerConfigured", properties.isConfigured());
        stats.put("webhookSecretConfigured", properties.hasWebhookSecret());
        return stats;
    }

    private PaymentView toView(Payment p) {
        List<EventView> events = p.getId() == null ? List.of()
                : eventRepository.findByPaymentIdOrderByCreatedAtAscIdAsc(p.getId()).stream()
                .map(e -> new EventView(e.getProviderPaymentId(), e.getProviderStatus(), e.getSource(), e.getCreatedAt())).toList();
        return new PaymentView(p.getId(), p.getOrderId(), p.getUserId(), p.getStatus().name(), p.getStatusDetail(),
                p.getAmount(), p.getCurrency(), p.getExternalPaymentId(), p.getPreferenceId(), p.getPaymentMethod(),
                p.getPayerEmail(), p.getVerifiedAt(), p.getCreatedAt(), p.getUpdatedAt(), events);
    }
}
