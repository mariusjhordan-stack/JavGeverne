package com.cronos.payment.service;

import com.cronos.payment.client.OrderClient;
import com.cronos.payment.client.OrderClient.OrderSnapshot;
import com.cronos.payment.client.StripeClient;
import com.cronos.payment.config.StripeProperties;
import com.cronos.payment.entity.Payment;
import com.cronos.payment.entity.PaymentStatus;
import com.cronos.payment.repository.PaymentEventRepository;
import com.cronos.payment.repository.PaymentRepository;
import com.cronos.payment.security.ApiException;
import com.cronos.payment.security.RequestUser;
import com.cronos.payment.service.PaymentProcessor.PaymentInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final Set<String> PAYABLE = Set.of("PENDING", "PAYMENT_FAILED");
    private static final Duration MIN_SESSION_LIFETIME = Duration.ofMinutes(30);
    private static final Duration MAX_SESSION_LIFETIME = Duration.ofHours(24);

    public record CheckoutResponse(UUID orderId, String preferenceId, String checkoutUrl) {}

    public record PaymentView(UUID id, UUID orderId, UUID userId, String status, String statusDetail, BigDecimal amount,
                              String currency, String externalPaymentId, String preferenceId, String paymentMethod,
                              String payerEmail, LocalDateTime verifiedAt, LocalDateTime createdAt, LocalDateTime updatedAt,
                              List<EventView> events) {}

    public record EventView(String providerPaymentId, String providerStatus, String source, LocalDateTime at) {}

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository eventRepository;
    private final PaymentProcessor processor;
    private final StripeClient stripe;
    private final OrderClient orderClient;
    private final StripeProperties properties;
    private final WebhookSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentRepository paymentRepository, PaymentEventRepository eventRepository,
                          PaymentProcessor processor, StripeClient stripe, OrderClient orderClient,
                          StripeProperties properties, WebhookSignatureVerifier signatureVerifier,
                          ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.eventRepository = eventRepository;
        this.processor = processor;
        this.stripe = stripe;
        this.orderClient = orderClient;
        this.properties = properties;
        this.signatureVerifier = signatureVerifier;
        this.objectMapper = objectMapper;
    }

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

        JsonNode session = stripe.createCheckoutSession(buildSession(order), UUID.randomUUID().toString());
        payment.setAmount(order.total());
        payment.setCurrency(order.currency());
        payment.setPayerEmail(order.userEmail());
        payment.setPreferenceId(session.path("id").asText());
        payment.setCheckoutUrl(session.path("url").asText());
        try {
            paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict("CHECKOUT_IN_PROGRESS", "A checkout for this order was just started. Try again");
        }
        return new CheckoutResponse(orderId, payment.getPreferenceId(), payment.getCheckoutUrl());
    }

    private MultiValueMap<String, String> buildSession(OrderSnapshot order) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("mode", "payment");
        form.add("success_url", appendQuery(properties.returnUrl("success"),
                "order=" + order.id() + "&session_id={CHECKOUT_SESSION_ID}"));
        form.add("cancel_url", appendQuery(properties.returnUrl("failure"), "order=" + order.id()));
        form.add("client_reference_id", order.id().toString());
        form.add("customer_email", order.userEmail());
        form.add("metadata[orderId]", order.id().toString());
        form.add("payment_intent_data[metadata][orderId]", order.id().toString());

        int exponent;
        try {
            exponent = Currency.getInstance(order.currency()).getDefaultFractionDigits();
        } catch (IllegalArgumentException e) {
            throw ApiException.conflict("ORDER_NOT_PAYABLE", "The order currency is not supported");
        }
        if (exponent < 0) {
            throw ApiException.conflict("ORDER_NOT_PAYABLE", "The order currency is not supported");
        }
        BigDecimal lineTotal = BigDecimal.ZERO;
        String currency = order.currency().toLowerCase();
        for (int i = 0; i < order.items().size(); i++) {
            OrderClient.OrderItem item = order.items().get(i);
            long unitAmount;
            try {
                unitAmount = item.unitPrice().movePointRight(exponent).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
            } catch (ArithmeticException e) {
                throw ApiException.conflict("ORDER_NOT_PAYABLE", "An item price cannot be represented in the order currency");
            }
            String prefix = "line_items[" + i + "]";
            form.add(prefix + "[price_data][currency]", currency);
            form.add(prefix + "[price_data][unit_amount]", Long.toString(unitAmount));
            form.add(prefix + "[price_data][product_data][name]", item.productName());
            form.add(prefix + "[quantity]", Integer.toString(item.quantity()));
            lineTotal = lineTotal.add(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }
        if (lineTotal.compareTo(order.total()) != 0) {
            throw ApiException.conflict("ORDER_NOT_PAYABLE", "The order total does not match its items");
        }
        if (order.expiresAt() != null) {
            Instant now = Instant.now();
            Instant expiresAt = order.expiresAt().atZone(ZoneId.systemDefault()).toInstant();
            if (expiresAt.isBefore(now.plus(MIN_SESSION_LIFETIME).plusSeconds(5))) {
                throw ApiException.conflict("ORDER_RESERVATION_EXPIRING", "This order reservation is expiring. Place a new order to pay");
            }
            form.add("expires_at", Long.toString(min(expiresAt, now.plus(MAX_SESSION_LIFETIME)).getEpochSecond()));
        }
        return form;
    }

    private static String appendQuery(String url, String query) {
        return url + (url.contains("?") ? "&" : "?") + query;
    }

    private static Instant min(Instant left, Instant right) {
        return left.isBefore(right) ? left : right;
    }

    public void handleWebhook(String payload, String signature) {
        signatureVerifier.verify(payload, signature);
        if (!properties.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_PROVIDER_NOT_CONFIGURED",
                    "Payment provider is not configured");
        }
        JsonNode event;
        try {
            event = objectMapper.readTree(payload);
        } catch (JsonProcessingException e) {
            throw ApiException.badRequest("Invalid webhook payload");
        }
        String type = event.path("type").asText("");
        JsonNode object = event.path("data").path("object");
        if (type.startsWith("checkout.session.")) {
            String sessionId = object.path("id").asText("");
            if (sessionId.isBlank()) {
                throw ApiException.badRequest("Webhook is missing its checkout session");
            }
            String statusOverride = switch (type) {
                case "checkout.session.async_payment_failed" -> "failed";
                case "checkout.session.expired" -> "expired";
                case "checkout.session.async_payment_succeeded" -> "succeeded";
                default -> null;
            };
            JsonNode verified = stripe.getCheckoutSession(sessionId);
            if (verified == null) {
                log.warn("Stripe webhook references unknown checkout session {}", sessionId);
                return;
            }
            applySafely(toInfo(verified, statusOverride), "webhook");
        } else if ("charge.refunded".equals(type)) {
            JsonNode paymentIntentNode = object.path("payment_intent");
            String paymentIntent = paymentIntentNode.isTextual()
                    ? paymentIntentNode.asText() : paymentIntentNode.path("id").asText("");
            String orderId = object.path("metadata").path("orderId").asText("");
            if (orderId.isBlank() && !paymentIntent.isBlank()) {
                JsonNode intent = stripe.getPaymentIntent(paymentIntent);
                orderId = intent.path("metadata").path("orderId").asText("");
            }
            if (!orderId.isBlank()) {
                try {
                    reconcileRefunded(UUID.fromString(orderId));
                } catch (IllegalArgumentException e) {
                    log.warn("Stripe refund has invalid order reference {}", orderId);
                }
            } else {
                log.warn("Stripe refund for payment intent {} has no usable order reference", paymentIntent);
            }
        }
    }

    public PaymentView sync(UUID orderId, RequestUser user) {
        Payment payment = owned(orderId, user);
        if (properties.isConfigured() && payment.getPreferenceId() != null) {
            reconcile(orderId);
        }
        return toView(paymentRepository.findByOrderId(orderId).orElse(payment));
    }

    public void reconcile(UUID orderId) {
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            if (payment.getPreferenceId() == null || payment.getPreferenceId().isBlank()) {
                return;
            }
            JsonNode session = stripe.getCheckoutSession(payment.getPreferenceId());
            if (session != null) {
                applySafely(toInfo(session, null), "sync");
            }
        });
    }

    private void reconcileRefunded(UUID orderId) {
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            if (payment.getPreferenceId() == null || payment.getPreferenceId().isBlank()) {
                return;
            }
            JsonNode session = stripe.getCheckoutSession(payment.getPreferenceId());
            if (session != null) {
                applySafely(toInfo(session, "refunded"), "webhook");
            }
        });
    }

    private PaymentInfo toInfo(JsonNode session, String statusOverride) {
        String currency = session.path("currency").asText("").toUpperCase();
        int exponent;
        try {
            exponent = Currency.getInstance(currency).getDefaultFractionDigits();
        } catch (IllegalArgumentException e) {
            exponent = 2;
        }
        long amount = session.path("amount_total").asLong(-1);
        BigDecimal total = amount < 0 ? null : BigDecimal.valueOf(amount, exponent);
        String sessionStatus = session.path("status").asText("");
        String paymentStatus = session.path("payment_status").asText("");
        String normalizedStatus = statusOverride != null ? statusOverride
                : "expired".equals(sessionStatus) ? "expired"
                : "paid".equals(paymentStatus) ? "succeeded"
                : "complete".equals(sessionStatus) ? "processing" : "open";
        String externalReference = session.path("client_reference_id").asText(
                session.path("metadata").path("orderId").asText(null));
        String paymentMethod = session.path("payment_method_types").isArray()
                ? session.path("payment_method_types").path(0).asText(null) : null;
        String email = session.path("customer_details").path("email").asText(
                session.path("customer_email").asText(null));
        return new PaymentInfo(session.path("id").asText(), normalizedStatus, sessionStatus, total, currency,
                externalReference, paymentMethod, email);
    }

    private void applySafely(PaymentInfo info, String source) {
        try {
            processor.apply(info, source);
        } catch (DataIntegrityViolationException e) {
            log.info("Provider event {} was processed concurrently", info.id());
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
