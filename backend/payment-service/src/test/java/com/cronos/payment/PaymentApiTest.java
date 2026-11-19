package com.cronos.payment;

import com.cronos.payment.entity.Payment;
import com.cronos.payment.entity.PaymentStatus;
import com.cronos.payment.repository.PaymentEventRepository;
import com.cronos.payment.repository.PaymentRepository;
import com.cronos.payment.service.WebhookSignatureVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentApiTest {

    static final String TOKEN = "test-internal-token-0123456789abcdef0123";
    static final String SECRET = "whsec_test-webhook-secret-key";
    static final ObjectMapper JSON = new ObjectMapper();
    static final Map<String, String> orders = new ConcurrentHashMap<>();
    static final Map<String, ObjectNode> sessions = new ConcurrentHashMap<>();
    static final List<String> orderResults = new CopyOnWriteArrayList<>();
    static final List<Map<String, String>> checkoutForms = new CopyOnWriteArrayList<>();
    static final AtomicInteger sessionIds = new AtomicInteger();
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
            server.createContext("/v1/checkout/sessions", exchange -> handleStripe(exchange));
            server.createContext("/v1/payment_intents", exchange -> handlePaymentIntent(exchange));
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static void handlePaymentIntent(HttpExchange exchange) throws IOException {
        if (!"Bearer sk_test_unit-test-key".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
            send(exchange, 401, "{}");
            return;
        }
        String id = exchange.getRequestURI().getPath().substring(exchange.getRequestURI().getPath().lastIndexOf('/') + 1);
        ObjectNode intent = JSON.createObjectNode();
        intent.put("id", id);
        sessions.values().stream()
                .filter(session -> ("pi_" + session.path("id").asText()).equals(id))
                .findFirst()
                .ifPresent(session -> intent.putObject("metadata")
                        .put("orderId", session.path("metadata").path("orderId").asText()));
        send(exchange, 200, intent.toString());
    }

    static void handleStripe(HttpExchange exchange) throws IOException {
        if (!"Bearer sk_test_unit-test-key".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
            send(exchange, 401, "{}");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        if ("POST".equals(exchange.getRequestMethod())) {
            Map<String, String> form = parseForm(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            checkoutForms.add(form);
            String id = "cs_test_" + sessionIds.incrementAndGet();
            ObjectNode session = JSON.createObjectNode();
            session.put("id", id);
            session.put("url", "https://stripe.test/checkout/" + id);
            session.put("status", "open");
            session.put("payment_status", "unpaid");
            session.put("amount_total", Long.parseLong(form.get("line_items[0][price_data][unit_amount]"))
                    * Long.parseLong(form.get("line_items[0][quantity]")));
            session.put("currency", form.get("line_items[0][price_data][currency]"));
            session.put("client_reference_id", form.get("client_reference_id"));
            session.put("customer_email", form.get("customer_email"));
            session.put("payment_intent", "pi_" + id);
            session.putArray("payment_method_types").add("card");
            session.putObject("metadata").put("orderId", form.get("metadata[orderId]"));
            session.putObject("customer_details").put("email", form.get("customer_email"));
            sessions.put(id, session);
            send(exchange, 200, session.toString());
            return;
        }
        String query = exchange.getRequestURI().getRawQuery();
        if (path.equals("/v1/checkout/sessions")) {
            Map<String, String> params = parseForm(query == null ? "" : query);
            String orderId = params.get("client_reference_id");
            String paymentIntent = params.get("payment_intent");
            List<String> matching = sessions.values().stream()
                    .filter(s -> orderId == null || orderId.equals(s.path("client_reference_id").asText()))
                    .filter(s -> paymentIntent == null || paymentIntent.equals(s.path("payment_intent").asText()))
                    .map(JsonNode::toString).toList();
            send(exchange, 200, "{\"data\":[" + String.join(",", matching) + "]}");
            return;
        }
        String id = path.substring(path.lastIndexOf('/') + 1);
        ObjectNode session = sessions.get(id);
        if (session == null) send(exchange, 404, "{}");
        else send(exchange, 200, session.toString());
    }

    static Map<String, String> parseForm(String form) {
        Map<String, String> values = new ConcurrentHashMap<>();
        for (String pair : form.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                values.put(UriUtils.decode(parts[0], StandardCharsets.UTF_8), UriUtils.decode(parts[1], StandardCharsets.UTF_8));
            }
        }
        return values;
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
        registry.add("stripe.api-base-url", () -> url);
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
        orders.clear();
        sessions.clear();
        orderResults.clear();
        checkoutForms.clear();
        orderServiceDown = false;
    }

    MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, UUID user, String role) {
        return request.header("X-Internal-Token", TOKEN).header("X-User-Id", user.toString())
                .header("X-User-Email", "buyer@cronos.test").header("X-User-Role", role)
                .contentType(MediaType.APPLICATION_JSON);
    }

    UUID newOrder(UUID owner, String status, String total) {
        UUID id = UUID.randomUUID();
        orders.put(id.toString(), "{\"id\":\"" + id + "\",\"userId\":\"" + owner + "\",\"userEmail\":\"buyer@cronos.test\",\"status\":\"" + status
                + "\",\"total\":" + total + ",\"currency\":\"USD\",\"expiresAt\":\"2999-01-01T00:00:00\",\"items\":[{\"productName\":\"Rolex Submariner Date\","
                + "\"quantity\":1,\"unitPrice\":" + total + "}]}");
        return id;
    }

    String checkout(UUID owner, UUID order) throws Exception {
        String response = mvc.perform(as(post("/api/payments/preference"), owner, "USER")
                        .content("{\"orderId\":\"" + order + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JSON.readTree(response).path("preferenceId").asText();
    }

    void setSession(String id, String status, String amount, String currency) {
        ObjectNode session = sessions.get(id);
        session.put("status", "open".equals(status) ? "open" : "complete");
        session.put("payment_status", "succeeded".equals(status) ? "paid" : "unpaid");
        session.put("amount_total", new java.math.BigDecimal(amount).movePointRight(2).longValueExact());
        session.put("currency", currency.toLowerCase());
    }

    MockHttpServletRequestBuilder signedWebhook(String type, String objectJson, long timestamp, String secret) throws Exception {
        String payload = "{\"id\":\"evt_test_" + UUID.randomUUID() + "\",\"type\":\"" + type
                + "\",\"data\":{\"object\":" + objectJson + "}}";
        String signature = java.util.HexFormat.of().formatHex(
                WebhookSignatureVerifier.hmac(timestamp + "." + payload, secret));
        return post("/api/payments/webhook").header("stripe-signature", "t=" + timestamp + ",v1=" + signature)
                .contentType(MediaType.APPLICATION_JSON).content(payload);
    }

    MockHttpServletRequestBuilder webhook(String type, String objectJson) throws Exception {
        return signedWebhook(type, objectJson, System.currentTimeMillis() / 1000, SECRET);
    }

    PaymentStatus statusOf(UUID order) {
        return payments.findByOrderId(order).orElseThrow().getStatus();
    }

    @Test
    void checkoutUsesServerOrderPricesAndStripeHostedCheckout() throws Exception {
        UUID order = newOrder(alice, "PENDING", "9150.00");
        mvc.perform(as(post("/api/payments/preference"), alice, "USER")
                        .content("{\"orderId\":\"" + order + "\",\"totalAmount\":1,\"currency\":\"XXX\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").value(org.hamcrest.Matchers.startsWith("https://stripe.test/checkout/cs_test_")));

        Map<String, String> form = checkoutForms.get(0);
        assertThat(form.get("mode")).isEqualTo("payment");
        assertThat(form.get("line_items[0][price_data][unit_amount]")).isEqualTo("915000");
        assertThat(form.get("line_items[0][quantity]")).isEqualTo("1");
        assertThat(form.get("client_reference_id")).isEqualTo(order.toString());
        assertThat(form.get("success_url")).contains("/checkout/success?order=" + order + "&session_id={CHECKOUT_SESSION_ID}");
        assertThat(form.get("cancel_url")).contains("/checkout/failure?order=" + order);
        assertThat(form.get("expires_at")).isNotBlank();
        Payment stored = payments.findByOrderId(order).orElseThrow();
        assertThat(stored.getAmount()).isEqualByComparingTo("9150.00");
        assertThat(stored.getUserId()).isEqualTo(alice);
    }

    @Test
    void checkoutIsOnlyForTheOwnerOfAPayableOrder() throws Exception {
        UUID order = newOrder(alice, "PENDING", "100.00");
        mvc.perform(post("/api/payments/preference").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"" + order + "\"}")).andExpect(status().isUnauthorized());
        mvc.perform(as(post("/api/payments/preference"), bob, "USER").content("{\"orderId\":\"" + order + "\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(as(post("/api/payments/preference"), alice, "USER")
                        .content("{\"orderId\":\"" + newOrder(alice, "PAID", "100.00") + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void webhooksRequireAValidFreshStripeSignature() throws Exception {
        UUID order = newOrder(alice, "PENDING", "500.00");
        String session = checkout(alice, order);
        String object = "{\"id\":\"" + session + "\"}";
        mvc.perform(post("/api/payments/webhook").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/payments/webhook").header("stripe-signature", "t=1,v1=deadbeef")
                .contentType(MediaType.APPLICATION_JSON).content("{\"id\":\"evt\",\"type\":\"checkout.session.completed\",\"data\":{\"object\":{\"id\":\""
                        + session + "\"}}}")).andExpect(status().isUnauthorized());
        mvc.perform(signedWebhook("checkout.session.completed", object, System.currentTimeMillis() / 1000, "wrong-secret"))
                .andExpect(status().isUnauthorized());
        mvc.perform(signedWebhook("checkout.session.completed", object, 1, SECRET)).andExpect(status().isUnauthorized());
        assertThat(statusOf(order)).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void verifiedPaymentIsAppliedOnceAndWrongAmountsOrCurrenciesAreIgnored() throws Exception {
        UUID order = newOrder(alice, "PENDING", "9150.00");
        String session = checkout(alice, order);
        setSession(session, "succeeded", "1.00", "USD");
        mvc.perform(webhook("checkout.session.completed", "{\"id\":\"" + session + "\"}")).andExpect(status().isOk());
        assertThat(statusOf(order)).isEqualTo(PaymentStatus.PENDING);

        setSession(session, "succeeded", "9150.00", "EUR");
        mvc.perform(webhook("checkout.session.completed", "{\"id\":\"" + session + "\"}")).andExpect(status().isOk());
        assertThat(statusOf(order)).isEqualTo(PaymentStatus.PENDING);

        setSession(session, "succeeded", "9150.00", "USD");
        mvc.perform(webhook("checkout.session.completed", "{\"id\":\"" + session + "\"}")).andExpect(status().isOk());
        mvc.perform(webhook("checkout.session.completed", "{\"id\":\"" + session + "\"}")).andExpect(status().isOk());
        Payment payment = payments.findByOrderId(order).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getVerifiedAt()).isNotNull();
        assertThat(payment.getExternalPaymentId()).isEqualTo(session);
        assertThat(orderResults).containsExactly(order + ":APPROVED:" + session);
        assertThat(events.findByPaymentIdOrderByCreatedAtAscIdAsc(payment.getId())).hasSize(1);
    }

    @Test
    void pendingPaymentCanSucceedAndRefundIsReported() throws Exception {
        UUID order = newOrder(alice, "PENDING", "800.00");
        String session = checkout(alice, order);
        setSession(session, "processing", "800.00", "USD");
        mvc.perform(webhook("checkout.session.completed", "{\"id\":\"" + session + "\"}")).andExpect(status().isOk());
        assertThat(statusOf(order)).isEqualTo(PaymentStatus.IN_PROCESS);

        setSession(session, "succeeded", "800.00", "USD");
        mvc.perform(webhook("checkout.session.async_payment_succeeded", "{\"id\":\"" + session + "\"}"))
                .andExpect(status().isOk());
        mvc.perform(webhook("charge.refunded", "{\"id\":\"ch_test_1\",\"payment_intent\":\"pi_" + session + "\"}"))
                .andExpect(status().isOk());
        assertThat(statusOf(order)).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(orderResults).containsExactly(order + ":PENDING:" + session, order + ":APPROVED:" + session,
                order + ":REFUNDED:" + session);
    }

    @Test
    void failedOrderServiceCallIsRetriedAndPaymentReadsArePrivate() throws Exception {
        UUID order = newOrder(alice, "PENDING", "300.00");
        String session = checkout(alice, order);
        setSession(session, "succeeded", "300.00", "USD");
        orderServiceDown = true;
        mvc.perform(webhook("checkout.session.completed", "{\"id\":\"" + session + "\"}"))
                .andExpect(status().isServiceUnavailable());
        assertThat(statusOf(order)).isEqualTo(PaymentStatus.PENDING);
        orderServiceDown = false;
        mvc.perform(as(post("/api/payments/order/" + order + "/sync"), bob, "USER")).andExpect(status().isNotFound());
        mvc.perform(as(post("/api/payments/order/" + order + "/sync"), alice, "USER"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        assertThat(orderResults).containsExactly(order + ":APPROVED:" + session);
    }
}
