package com.cronos.payment;

import com.cronos.payment.entity.Payment;
import com.cronos.payment.entity.PaymentStatus;
import com.cronos.payment.repository.PaymentEventRepository;
import com.cronos.payment.repository.PaymentRepository;
import com.cronos.payment.service.WebhookSignatureVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * payment-service against HTTP stand-ins for the Mercado Pago API and order-service. These tests
 * prove our handling of what the provider reports; they do not prove anything about Mercado Pago itself.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentApiTest {

    static final String TOKEN = "test-internal-token-0123456789abcdef0123";
    static final String SECRET = "test-webhook-secret-key";
    static final ObjectMapper JSON = new ObjectMapper();

    static final Map<String, String> orders = new ConcurrentHashMap<>();
    static final Map<String, String> providerPayments = new ConcurrentHashMap<>();
    static final List<String> orderResults = new CopyOnWriteArrayList<>();
    static final List<JsonNode> preferences = new CopyOnWriteArrayList<>();
    static final List<String> providerAuth = new CopyOnWriteArrayList<>();
    static volatile boolean orderServiceDown;
    static final HttpServer peers = startPeers();

    static HttpServer startPeers() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/internal/orders/", exchange -> {
                String[] parts = exchange.getRequestURI().getPath().split("/");
                String orderId = parts[3];
                if (!TOKEN.equals(exchange.getRequestHeaders().getFirst("X-Internal-Token"))) {
                    send(exchange, 401, "{}");
                } else if (parts.length > 4 && parts[4].equals("payment-result")) {
                    JsonNode body = JSON.readTree(exchange.getRequestBody().readAllBytes());
                    if (orderServiceDown) {
                        send(exchange, 503, "{}");
                    } else {
                        orderResults.add(orderId + ":" + body.get("result").asText() + ":" + body.get("paymentId").asText());
                        send(exchange, 200, "{}");
                    }
                } else if (orders.containsKey(orderId)) {
                    send(exchange, 200, orders.get(orderId));
                } else {
                    send(exchange, 404, "{}");
                }
            });
            server.createContext("/checkout/preferences", exchange -> {
                providerAuth.add(exchange.getRequestHeaders().getFirst("Authorization"));
                preferences.add(JSON.readTree(exchange.getRequestBody().readAllBytes()));
                send(exchange, 201, "{\"id\":\"PREF-1\",\"init_point\":\"https://mp.test/init\",\"sandbox_init_point\":\"https://mp.test/sandbox\"}");
            });
            server.createContext("/v1/payments/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                if (path.endsWith("/search")) {
                    String reference = exchange.getRequestURI().getQuery().split("external_reference=")[1].split("&")[0];
                    String results = String.join(",", providerPayments.values().stream().filter(p -> p.contains(reference)).toList());
                    send(exchange, 200, "{\"results\":[" + results + "]}");
                } else {
                    String id = path.substring(path.lastIndexOf('/') + 1);
                    if (providerPayments.containsKey(id)) send(exchange, 200, providerPayments.get(id));
                    else send(exchange, 404, "{}");
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String url = "http://127.0.0.1:" + peers.getAddress().getPort();
        registry.add("app.order-service-url", () -> url);
        registry.add("mercadopago.api-base-url", () -> url);
    }

    @AfterAll
    static void stop() {
        peers.stop(0);
    }

    @Autowired MockMvc mvc;
    @Autowired PaymentRepository payments;
    @Autowired PaymentEventRepository events;

    final UUID alice = UUID.randomUUID();
    final UUID bob = UUID.randomUUID();

    @BeforeEach
    void reset() {
        orderResults.clear();
        preferences.clear();
        providerAuth.clear();
        orderServiceDown = false;
    }

    MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder b, UUID user, String role) {
        return b.header("X-Internal-Token", TOKEN).header("X-User-Id", user.toString())
                .header("X-User-Email", "buyer@cronos.test").header("X-User-Role", role).contentType(MediaType.APPLICATION_JSON);
    }

    UUID newOrder(UUID owner, String status, String total) {
        UUID id = UUID.randomUUID();
        orders.put(id.toString(), "{\"id\":\"" + id + "\",\"userId\":\"" + owner + "\",\"userEmail\":\"buyer@cronos.test\",\"status\":\"" + status
                + "\",\"total\":" + total + ",\"currency\":\"USD\",\"expiresAt\":\"2999-01-01T00:00:00\",\"items\":[{\"productName\":\"Rolex Submariner Date\","
                + "\"quantity\":1,\"unitPrice\":" + total + "}]}");
        return id;
    }

    UUID checkedOutOrder(String total) throws Exception {
        UUID order = newOrder(alice, "PENDING", total);
        mvc.perform(as(post("/api/payments/preference"), alice, "USER").content("{\"orderId\":\"" + order + "\"}")).andExpect(status().isOk());
        return order;
    }

    void providerSays(String paymentId, String status, String amount, String currency, UUID order) {
        providerPayments.put(paymentId, "{\"id\":" + paymentId + ",\"status\":\"" + status + "\",\"status_detail\":\"detail\",\"transaction_amount\":"
                + amount + ",\"currency_id\":\"" + currency + "\",\"external_reference\":\"" + order + "\",\"payment_method_id\":\"visa\",\"payer\":{\"email\":\"p@x.test\"}}");
    }

    MockHttpServletRequestBuilder signedWebhook(String paymentId, long ts, String secret) {
        String requestId = "req-" + paymentId;
        String signature = HexFormat.of().formatHex(WebhookSignatureVerifier.hmac("id:" + paymentId + ";request-id:" + requestId + ";ts:" + ts + ";", secret));
        return post("/api/payments/webhook").param("data.id", paymentId).param("type", "payment")
                .header("x-request-id", requestId).header("x-signature", "ts=" + ts + ",v1=" + signature)
                .contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"payment.updated\",\"type\":\"payment\",\"data\":{\"id\":\"" + paymentId + "\"}}");
    }

    MockHttpServletRequestBuilder webhook(String paymentId) {
        return signedWebhook(paymentId, System.currentTimeMillis() / 1000, SECRET);
    }

    PaymentStatus statusOf(UUID order) {
        return payments.findByOrderId(order).orElseThrow().getStatus();
    }

    // ---------- checkout ----------

    @Test
    void checkoutUsesTheOrderTotal_neverTheClients() throws Exception {
        UUID order = newOrder(alice, "PENDING", "9150.00");
        mvc.perform(as(post("/api/payments/preference"), alice, "USER")
                        .content("{\"orderId\":\"" + order + "\",\"totalAmount\":1.00,\"items\":[{\"title\":\"x\",\"quantity\":1,\"unitPrice\":1.00}],\"currency\":\"USD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferenceId").value("PREF-1"))
                .andExpect(jsonPath("$.checkoutUrl").value("https://mp.test/sandbox"));

        JsonNode sent = preferences.get(0);
        assertThat(sent.get("items").get(0).get("unit_price").decimalValue()).isEqualByComparingTo("9150.00");
        assertThat(sent.get("external_reference").asText()).isEqualTo(order.toString());
        assertThat(sent.get("back_urls").get("success").asText()).isEqualTo("https://shop.cronos.test/checkout/success?order=" + order);
        assertThat(sent.get("back_urls").get("failure").asText()).startsWith("https://shop.cronos.test/checkout/failure");
        assertThat(sent.toString()).doesNotContain("localhost");
        assertThat(sent.get("auto_return").asText()).isEqualTo("approved");
        assertThat(sent.get("notification_url").asText()).isEqualTo("https://shop.cronos.test/api/payments/webhook");
        assertThat(sent.get("expires").asBoolean()).isTrue();
        assertThat(providerAuth.get(0)).isEqualTo("Bearer TEST-access-token");

        Payment stored = payments.findByOrderId(order).orElseThrow();
        assertThat(stored.getAmount()).isEqualByComparingTo("9150.00");
        assertThat(stored.getUserId()).isEqualTo(alice);
    }

    @Test
    void checkoutIsOnlyForTheOwnerOfAPayableOrder() throws Exception {
        UUID order = newOrder(alice, "PENDING", "100.00");
        mvc.perform(post("/api/payments/preference").contentType(MediaType.APPLICATION_JSON).content("{\"orderId\":\"" + order + "\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(as(post("/api/payments/preference"), bob, "USER").content("{\"orderId\":\"" + order + "\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(as(post("/api/payments/preference"), alice, "USER").content("{\"orderId\":\"" + newOrder(alice, "PAID", "100.00") + "\"}"))
                .andExpect(status().isConflict());
        mvc.perform(as(post("/api/payments/preference"), alice, "USER").content("{\"orderId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(as(post("/api/payments/preference"), alice, "USER").content("{}")).andExpect(status().isBadRequest());
        assertThat(preferences).isEmpty();
    }

    // ---------- webhook authentication ----------

    @Test
    void webhookWithoutValidSignatureIsRefused() throws Exception {
        UUID order = checkedOutOrder("500.00");
        providerSays("7001", "approved", "500.00", "USD", order);
        long now = System.currentTimeMillis() / 1000;

        mvc.perform(post("/api/payments/webhook").param("data.id", "7001").param("type", "payment")
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/payments/webhook").param("data.id", "7001").param("type", "payment")
                .header("x-signature", "ts=" + now + ",v1=deadbeef").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(signedWebhook("7001", now, "another-secret")).andExpect(status().isUnauthorized());
        // Correctly signed but replayed outside the accepted window.
        mvc.perform(signedWebhook("7001", now - 7200, SECRET)).andExpect(status().isUnauthorized());
        mvc.perform(signedWebhook("7001", now + 7200, SECRET)).andExpect(status().isUnauthorized());
        // A signature for one payment cannot be reused for another.
        providerSays("7002", "approved", "500.00", "USD", order);
        String stolen = HexFormat.of().formatHex(WebhookSignatureVerifier.hmac("id:7001;request-id:r;ts:" + now + ";", SECRET));
        mvc.perform(post("/api/payments/webhook").param("data.id", "7002").param("type", "payment").header("x-request-id", "r")
                .header("x-signature", "ts=" + now + ",v1=" + stolen).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        // Non-payment topics need a valid signature too.
        mvc.perform(post("/api/payments/webhook").param("data.id", "7001").param("type", "merchant_order")
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());

        assertThat(statusOf(order)).isEqualTo(PaymentStatus.PENDING);
        assertThat(orderResults).isEmpty();
    }

    @Test
    void millisecondTimestampsAreAccepted() throws Exception {
        UUID order = checkedOutOrder("500.00");
        providerSays("7010", "approved", "500.00", "USD", order);
        mvc.perform(signedWebhook("7010", System.currentTimeMillis(), SECRET)).andExpect(status().isOk());
        assertThat(statusOf(order)).isEqualTo(PaymentStatus.APPROVED);
    }

    // ---------- payment outcomes ----------

    @Test
    void approvedPaymentIsVerifiedWithTheProviderAndAppliedOnce() throws Exception {
        UUID order = checkedOutOrder("9150.00");
        providerSays("7101", "approved", "9150.00", "USD", order);
        mvc.perform(webhook("7101")).andExpect(status().isOk());
        mvc.perform(webhook("7101")).andExpect(status().isOk());
        mvc.perform(webhook("7101")).andExpect(status().isOk());

        Payment payment = payments.findByOrderId(order).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getVerifiedAt()).isNotNull();
        assertThat(payment.getExternalPaymentId()).isEqualTo("7101");
        assertThat(orderResults).containsExactly(order + ":APPROVED:7101");
        assertThat(events.findByPaymentIdOrderByCreatedAtAscIdAsc(payment.getId())).hasSize(1);
    }

    @Test
    void pendingThenApprovedOnTheSamePayment() throws Exception {
        UUID order = checkedOutOrder("6550.00");
        providerSays("7201", "in_process", "6550.00", "USD", order);
        mvc.perform(webhook("7201")).andExpect(status().isOk());
        assertThat(statusOf(order)).isEqualTo(PaymentStatus.IN_PROCESS);
        providerSays("7201", "approved", "6550.00", "USD", order);
        mvc.perform(webhook("7201")).andExpect(status().isOk());
        assertThat(statusOf(order)).isEqualTo(PaymentStatus.APPROVED);
        assertThat(orderResults).containsExactly(order + ":PENDING:7201", order + ":APPROVED:7201");
    }

    @Test
    void pendingThenRejected_thenASecondAttemptApproved() throws Exception {
        UUID order = checkedOutOrder("5250.00");
        providerSays("7301", "pending", "5250.00", "USD", order);
        mvc.perform(webhook("7301")).andExpect(status().isOk());
        providerSays("7301", "rejected", "5250.00", "USD", order);
        mvc.perform(webhook("7301")).andExpect(status().isOk());
        assertThat(statusOf(order)).isEqualTo(PaymentStatus.REJECTED);
        assertThat(payments.findByOrderId(order).orElseThrow().getVerifiedAt()).isNull();

        providerSays("7302", "approved", "5250.00", "USD", order);
        mvc.perform(webhook("7302")).andExpect(status().isOk());
        assertThat(statusOf(order)).isEqualTo(PaymentStatus.APPROVED);
        // A late notification about the failed attempt must not undo the approval.
        providerSays("7301", "cancelled", "5250.00", "USD", order);
        mvc.perform(webhook("7301")).andExpect(status().isOk());
        assertThat(statusOf(order)).isEqualTo(PaymentStatus.APPROVED);
        assertThat(orderResults).containsExactly(order + ":PENDING:7301", order + ":REJECTED:7301", order + ":APPROVED:7302");
    }

    @Test
    void refundOfTheApprovedPaymentIsApplied() throws Exception {
        UUID order = checkedOutOrder("800.00");
        providerSays("7401", "approved", "800.00", "USD", order);
        mvc.perform(webhook("7401")).andExpect(status().isOk());
        providerSays("7401", "refunded", "800.00", "USD", order);
        mvc.perform(webhook("7401")).andExpect(status().isOk());
        assertThat(statusOf(order)).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(orderResults).containsExactly(order + ":APPROVED:7401", order + ":REFUNDED:7401");
    }

    @Test
    void wrongAmountCurrencyOrReferenceNeverMarksPaid() throws Exception {
        UUID order = checkedOutOrder("9150.00");
        providerSays("7501", "approved", "1.00", "USD", order);
        mvc.perform(webhook("7501")).andExpect(status().isOk());
        providerSays("7502", "approved", "9150.00", "PEN", order);
        mvc.perform(webhook("7502")).andExpect(status().isOk());
        assertThat(statusOf(order)).isEqualTo(PaymentStatus.PENDING);

        // An approved payment that points at an order for which we never started a checkout.
        UUID stranger = newOrder(alice, "PENDING", "9150.00");
        providerSays("7503", "approved", "0.50", "USD", stranger);
        mvc.perform(webhook("7503")).andExpect(status().isOk());
        assertThat(payments.findByOrderId(stranger)).isEmpty();
        // Signed notification about a payment the provider does not know.
        mvc.perform(webhook("7599")).andExpect(status().isOk());
        assertThat(orderResults).isEmpty();
    }

    @Test
    void ifTheOrderServiceIsDownNothingIsStoredAndTheRetrySucceeds() throws Exception {
        UUID order = checkedOutOrder("300.00");
        providerSays("7601", "approved", "300.00", "USD", order);
        orderServiceDown = true;
        mvc.perform(webhook("7601")).andExpect(status().isServiceUnavailable());
        assertThat(statusOf(order)).isEqualTo(PaymentStatus.PENDING);
        orderServiceDown = false;
        mvc.perform(webhook("7601")).andExpect(status().isOk());
        assertThat(statusOf(order)).isEqualTo(PaymentStatus.APPROVED);
        assertThat(orderResults).containsExactly(order + ":APPROVED:7601");
    }

    // ---------- reading and reconciliation ----------

    @Test
    void syncAsksTheProvider_andPaymentsArePrivate() throws Exception {
        UUID order = checkedOutOrder("450.00");
        providerSays("7701", "approved", "450.00", "USD", order);
        mvc.perform(as(post("/api/payments/order/" + order + "/sync"), bob, "USER")).andExpect(status().isNotFound());
        mvc.perform(as(get("/api/payments/order/" + order), bob, "USER")).andExpect(status().isNotFound());
        mvc.perform(get("/api/payments/order/" + order)).andExpect(status().isUnauthorized());
        assertThat(statusOf(order)).isEqualTo(PaymentStatus.PENDING);

        mvc.perform(as(post("/api/payments/order/" + order + "/sync"), alice, "USER"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.events[0].source").value("sync"));
        mvc.perform(as(get("/api/payments/order/" + order), alice, "USER")).andExpect(jsonPath("$.status").value("APPROVED"));
        assertThat(orderResults).containsExactly(order + ":APPROVED:7701");

        mvc.perform(as(get("/api/admin/payments"), alice, "USER")).andExpect(status().isForbidden());
        mvc.perform(as(get("/api/admin/payments").param("status", "APPROVED"), bob, "ADMIN")).andExpect(status().isOk());
        mvc.perform(as(get("/api/admin/payments/stats"), bob, "ADMIN")).andExpect(jsonPath("$.providerConfigured").value(true));
    }
}
