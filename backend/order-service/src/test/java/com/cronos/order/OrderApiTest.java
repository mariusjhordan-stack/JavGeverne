package com.cronos.order;

import com.cronos.order.entity.OrderStatus;
import com.cronos.order.repository.OrderRepository;
import com.cronos.order.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * order-service against HTTP stand-ins for catalog and notification that speak the real wire
 * contract, so the JSON the clients send is asserted rather than mocked away.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderApiTest {

    static final String TOKEN = "test-internal-token-0123456789abcdef0123";
    static final String PRODUCT = "a1b2c3d4-0001-4000-8000-000000000001";
    static final ObjectMapper JSON = new ObjectMapper();
    static final List<String> catalogCalls = new CopyOnWriteArrayList<>();
    static final List<JsonNode> notifications = new CopyOnWriteArrayList<>();
    static volatile boolean outOfStock;
    static volatile boolean confirmFails;
    static final HttpServer peers = startPeers();

    static HttpServer startPeers() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/internal/stock/reservations", exchange -> {
                String path = exchange.getRequestURI().getPath();
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                boolean authorized = TOKEN.equals(exchange.getRequestHeaders().getFirst("X-Internal-Token"));
                catalogCalls.add(exchange.getRequestMethod() + " " + path + " " + body);
                String response;
                int status = 200;
                if (!authorized) {
                    status = 401;
                    response = "{\"error\":\"Authentication required\"}";
                } else if (path.endsWith("/release")) {
                    response = "{\"released\":1}";
                } else if (path.endsWith("/confirm")) {
                    status = confirmFails ? 409 : 200;
                    response = "{\"confirmed\":true}";
                } else if (outOfStock) {
                    status = 409;
                    response = "{\"error\":\"Insufficient stock for Submariner Date\",\"code\":\"INSUFFICIENT_STOCK\"}";
                } else {
                    JsonNode request = JSON.readTree(body);
                    int quantity = request.get("items").get(0).get("quantity").asInt();
                    response = "{\"orderId\":\"" + request.get("orderId").asText() + "\",\"currency\":\"USD\",\"items\":[{"
                            + "\"productId\":\"" + request.get("items").get(0).get("productId").asText() + "\","
                            + "\"name\":\"Submariner Date\",\"brandName\":\"Rolex\",\"imageUrl\":\"/images/sub.webp\","
                            + "\"unitPrice\":9150.00,\"quantity\":" + quantity + ",\"lineTotal\":" + (9150 * quantity) + ".00}],"
                            + "\"total\":" + (9150 * quantity) + ".00}";
                }
                send(exchange, status, response);
            });
            server.createContext("/internal/notifications", exchange -> {
                notifications.add(JSON.readTree(exchange.getRequestBody().readAllBytes()));
                send(exchange, 200, "{}");
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static void send(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String url = "http://127.0.0.1:" + peers.getAddress().getPort();
        registry.add("app.catalog-service-url", () -> url);
        registry.add("app.notification-service-url", () -> url);
    }

    @AfterAll
    static void stop() {
        peers.stop(0);
    }

    @Autowired MockMvc mvc;
    @Autowired OrderRepository orders;
    @Autowired OrderService orderService;

    final UUID alice = UUID.randomUUID();
    final UUID bob = UUID.randomUUID();

    @BeforeEach
    void reset() {
        catalogCalls.clear();
        notifications.clear();
        outOfStock = false;
        confirmFails = false;
    }

    MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder b, UUID userId, String role) {
        return b.header("X-Internal-Token", TOKEN).header("X-User-Id", userId.toString())
                .header("X-User-Email", userId + "@cronos.test").header("X-User-Role", role)
                .contentType(MediaType.APPLICATION_JSON);
    }

    MockHttpServletRequestBuilder service(MockHttpServletRequestBuilder b) {
        return b.header("X-Internal-Token", TOKEN).contentType(MediaType.APPLICATION_JSON);
    }

    String orderBody(int quantity) {
        return "{\"items\":[{\"productId\":\"" + PRODUCT + "\",\"quantity\":" + quantity + "}],\"shippingAddress\":\"Av. Larco 123, Lima, Peru\"}";
    }

    String createOrder(UUID user) throws Exception {
        String response = mvc.perform(as(post("/api/orders"), user, "USER").content(orderBody(1)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JSON.readTree(response).get("id").asText();
    }

    String paymentResult(String result) {
        return "{\"result\":\"" + result + "\",\"paymentId\":\"mp-1\"}";
    }

    OrderStatus statusOf(String id) {
        return orders.findById(UUID.fromString(id)).orElseThrow().getStatus();
    }

    @Test
    void orderIsPricedByTheCatalog_notByTheClient() throws Exception {
        String tampered = "{\"items\":[{\"productId\":\"" + PRODUCT + "\",\"quantity\":2,\"unitPrice\":0.01,\"productName\":\"Free watch\"}],"
                + "\"shippingAddress\":\"Somewhere 1\",\"total\":0.02}";
        mvc.perform(as(post("/api/orders"), alice, "USER").content(tampered))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(18300.00))
                .andExpect(jsonPath("$.items[0].unitPrice").value(9150.00))
                .andExpect(jsonPath("$.items[0].productName").value("Rolex Submariner Date"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.userId").value(alice.toString()))
                .andExpect(jsonPath("$.expiresAt").exists());

        assertThat(catalogCalls).hasSize(1);
        JsonNode sent = JSON.readTree(catalogCalls.get(0).substring(catalogCalls.get(0).indexOf('{')));
        assertThat(catalogCalls.get(0)).startsWith("POST /internal/stock/reservations ");
        assertThat(sent.get("orderId").asText()).isNotBlank();
        assertThat(sent.get("items").get(0).get("productId").asText()).isEqualTo(PRODUCT);
        assertThat(sent.get("items").get(0).get("quantity").asInt()).isEqualTo(2);
        assertThat(sent.get("items").get(0).has("unitPrice")).isFalse();
        assertThat(notifications).extracting(n -> n.get("type").asText()).containsExactly("ORDER_CREATED");
    }

    @Test
    void invalidOrdersAreRejected() throws Exception {
        for (String body : List.of(
                "{\"items\":[],\"shippingAddress\":\"x\"}",
                "{\"items\":[{\"productId\":\"" + PRODUCT + "\",\"quantity\":0}],\"shippingAddress\":\"x\"}",
                "{\"items\":[{\"productId\":\"" + PRODUCT + "\",\"quantity\":-3}],\"shippingAddress\":\"x\"}",
                "{\"items\":[{\"productId\":\"" + PRODUCT + "\",\"quantity\":1}]}")) {
            mvc.perform(as(post("/api/orders"), alice, "USER").content(body)).andExpect(status().isBadRequest());
        }
        assertThat(catalogCalls).isEmpty();
    }

    @Test
    void insufficientStockIsReportedAndNothingIsStored() throws Exception {
        long before = orders.count();
        outOfStock = true;
        mvc.perform(as(post("/api/orders"), alice, "USER").content(orderBody(1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
        assertThat(orders.count()).isEqualTo(before);
    }

    @Test
    void anonymousAndForgedRequestsAreRejected() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(orderBody(1)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orders").header("X-User-Id", alice.toString()).header("X-User-Role", "ADMIN"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/orders").header("X-User-Id", alice.toString()).header("X-User-Role", "ADMIN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usersOnlySeeAndCancelTheirOwnOrders() throws Exception {
        String id = createOrder(alice);
        mvc.perform(as(get("/api/orders/" + id), alice, "USER")).andExpect(status().isOk());
        mvc.perform(as(get("/api/orders"), alice, "USER")).andExpect(jsonPath("$[*].id", hasItem(id)));

        mvc.perform(as(get("/api/orders/" + id), bob, "USER")).andExpect(status().isNotFound());
        mvc.perform(as(get("/api/orders"), bob, "USER")).andExpect(jsonPath("$[*].id", not(hasItem(id))));
        mvc.perform(as(post("/api/orders/" + id + "/cancel"), bob, "USER")).andExpect(status().isNotFound());
        assertThat(statusOf(id)).isEqualTo(OrderStatus.PENDING);

        mvc.perform(as(post("/api/orders/" + id + "/cancel"), alice, "USER"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(catalogCalls).anyMatch(c -> c.startsWith("POST /internal/stock/reservations/" + id + "/release"));
        assertThat(notifications).extracting(n -> n.get("type").asText()).contains("ORDER_CANCELLED");
    }

    @Test
    void nobodyCanMarkAnOrderPaidThroughThePublicApi() throws Exception {
        String id = createOrder(alice);
        String paid = "{\"status\":\"PAID\"}";
        // The old public endpoint is gone for users...
        mvc.perform(as(put("/api/orders/" + id + "/status"), alice, "USER").content(paid)).andExpect(status().is4xxClientError());
        // ...users cannot use the admin one...
        mvc.perform(as(put("/api/admin/orders/" + id + "/status"), alice, "USER").content(paid)).andExpect(status().isForbidden());
        // ...and not even an administrator may set payment states by hand.
        mvc.perform(as(put("/api/admin/orders/" + id + "/status"), bob, "ADMIN").content(paid))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("STATUS_NOT_ASSIGNABLE"));
        mvc.perform(as(put("/api/admin/orders/" + id + "/status"), bob, "ADMIN").content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
        // The internal endpoint needs the service token, which a browser never has.
        mvc.perform(post("/internal/orders/" + id + "/payment-result").contentType(MediaType.APPLICATION_JSON).content(paymentResult("APPROVED")))
                .andExpect(status().isUnauthorized());
        assertThat(statusOf(id)).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void approvedPaymentMarksPaid_confirmsStock_andIsIdempotent() throws Exception {
        String id = createOrder(alice);
        mvc.perform(service(post("/internal/orders/" + id + "/payment-result")).content(paymentResult("PENDING")))
                .andExpect(jsonPath("$.status").value("PAYMENT_PENDING"));
        mvc.perform(service(post("/internal/orders/" + id + "/payment-result")).content(paymentResult("APPROVED")))
                .andExpect(jsonPath("$.status").value("PAID")).andExpect(jsonPath("$.paymentId").value("mp-1"))
                .andExpect(jsonPath("$.paidAt").exists());
        mvc.perform(service(post("/internal/orders/" + id + "/payment-result")).content(paymentResult("APPROVED")))
                .andExpect(jsonPath("$.status").value("PAID"));
        // A late "rejected" or "pending" notification must not undo a paid order.
        mvc.perform(service(post("/internal/orders/" + id + "/payment-result")).content(paymentResult("REJECTED")))
                .andExpect(jsonPath("$.status").value("PAID"));
        mvc.perform(service(post("/internal/orders/" + id + "/payment-result")).content(paymentResult("PENDING")))
                .andExpect(jsonPath("$.status").value("PAID"));

        assertThat(catalogCalls.stream().filter(c -> c.contains("/confirm")).count()).isEqualTo(1);
        assertThat(notifications.stream().filter(n -> n.get("type").asText().equals("PAYMENT_APPROVED")).count()).isEqualTo(1);
        mvc.perform(as(post("/api/orders/" + id + "/cancel"), alice, "USER")).andExpect(status().isConflict());
        mvc.perform(as(get("/api/orders/" + id), alice, "USER"))
                .andExpect(jsonPath("$.history[*].to", contains("PENDING", "PAYMENT_PENDING", "PAID")));
    }

    @Test
    void rejectedPaymentCanBeRetried_thenRefunded() throws Exception {
        String id = createOrder(alice);
        mvc.perform(service(post("/internal/orders/" + id + "/payment-result")).content(paymentResult("REJECTED")))
                .andExpect(jsonPath("$.status").value("PAYMENT_FAILED"));
        assertThat(catalogCalls).noneMatch(c -> c.contains("/release"));
        mvc.perform(service(post("/internal/orders/" + id + "/payment-result")).content(paymentResult("APPROVED")))
                .andExpect(jsonPath("$.status").value("PAID"));
        mvc.perform(service(post("/internal/orders/" + id + "/payment-result")).content(paymentResult("REFUNDED")))
                .andExpect(jsonPath("$.status").value("REFUNDED"));
        assertThat(catalogCalls).anyMatch(c -> c.contains("/release"));
        assertThat(notifications).extracting(n -> n.get("type").asText())
                .containsSubsequence("PAYMENT_REJECTED", "PAYMENT_APPROVED", "ORDER_REFUNDED");
    }

    @Test
    void adminFulfilsPaidOrders_andSeesEverything() throws Exception {
        String id = createOrder(alice);
        mvc.perform(service(post("/internal/orders/" + id + "/payment-result")).content(paymentResult("APPROVED")));
        for (String step : List.of("PROCESSING", "SHIPPED", "DELIVERED")) {
            mvc.perform(as(put("/api/admin/orders/" + id + "/status"), bob, "ADMIN").content("{\"status\":\"" + step + "\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(step));
        }
        mvc.perform(as(put("/api/admin/orders/" + id + "/status"), bob, "ADMIN").content("{\"status\":\"PROCESSING\"}"))
                .andExpect(status().isConflict());
        mvc.perform(as(get("/api/admin/orders").param("status", "DELIVERED"), bob, "ADMIN"))
                .andExpect(jsonPath("$.content[*].id", hasItem(id)));
        mvc.perform(as(get("/api/admin/orders/stats"), bob, "ADMIN"))
                .andExpect(jsonPath("$.byStatus.DELIVERED", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.revenue", greaterThanOrEqualTo(9150.0)));
        mvc.perform(as(get("/api/admin/orders"), alice, "USER")).andExpect(status().isForbidden());
        mvc.perform(service(get("/internal/orders/purchased").param("userId", alice.toString()).param("productId", PRODUCT)))
                .andExpect(jsonPath("$.purchased").value(true));
        mvc.perform(service(get("/internal/orders/purchased").param("userId", bob.toString()).param("productId", PRODUCT)))
                .andExpect(jsonPath("$.purchased").value(false));
    }

    @Test
    void expiredUnpaidOrderIsCancelledAndStockReleased() throws Exception {
        String id = createOrder(alice);
        orderService.cancel(UUID.fromString(id), "system", "EXPIRED");
        assertThat(statusOf(id)).isEqualTo(OrderStatus.CANCELLED);
        assertThat(orders.findById(UUID.fromString(id)).orElseThrow().isStockReleasePending()).isFalse();
        assertThat(catalogCalls).anyMatch(c -> c.contains("/release"));
        // A payment approved after cancellation must not resurrect the order.
        mvc.perform(service(post("/internal/orders/" + id + "/payment-result")).content(paymentResult("APPROVED")))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
