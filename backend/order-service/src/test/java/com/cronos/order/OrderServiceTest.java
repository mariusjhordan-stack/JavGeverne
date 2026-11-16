package com.cronos.order;

import com.cronos.order.client.CatalogClient;
import com.cronos.order.entity.Order;
import com.cronos.order.entity.OrderItem;
import com.cronos.order.entity.OrderStatus;
import com.cronos.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private CatalogClient catalogClient;

    private final UUID userId = UUID.randomUUID();
    private final String userEmail = "buyer@test.com";

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    void createOrder_success() throws Exception {
        when(catalogClient.reserveStock(any(UUID.class), anyInt())).thenReturn(true);

        UUID productId = UUID.randomUUID();
        String body = """
                {
                    "items": [
                        {
                            "productId": "%s",
                            "productName": "Rolex Submariner",
                            "productImage": "https://images.com/rolex.jpg",
                            "quantity": 1,
                            "unitPrice": 12500.00
                        }
                    ],
                    "shippingAddress": "123 Watch Lane, Geneva"
                }
                """.formatted(productId);

        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Email", userEmail)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.userEmail").value(userEmail))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.total").value(12500.00))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productName").value("Rolex Submariner"))
                .andExpect(jsonPath("$.shippingAddress").value("123 Watch Lane, Geneva"));

        verify(catalogClient).reserveStock(productId, 1);
    }

    @Test
    void createOrder_multipleItems_calculatesTotal() throws Exception {
        when(catalogClient.reserveStock(any(UUID.class), anyInt())).thenReturn(true);

        UUID product1 = UUID.randomUUID();
        UUID product2 = UUID.randomUUID();
        String body = """
                {
                    "items": [
                        {
                            "productId": "%s",
                            "productName": "Rolex Submariner",
                            "quantity": 1,
                            "unitPrice": 12500.00
                        },
                        {
                            "productId": "%s",
                            "productName": "Omega Seamaster",
                            "quantity": 2,
                            "unitPrice": 6500.00
                        }
                    ],
                    "shippingAddress": "456 Luxury Ave"
                }
                """.formatted(product1, product2);

        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Email", userEmail)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(25500.00))
                .andExpect(jsonPath("$.items", hasSize(2)));

        verify(catalogClient, times(2)).reserveStock(any(UUID.class), anyInt());
    }

    @Test
    void createOrder_insufficientStock_returnsError() throws Exception {
        when(catalogClient.reserveStock(any(UUID.class), anyInt())).thenReturn(false);

        String body = """
                {
                    "items": [
                        {
                            "productId": "%s",
                            "productName": "Rare Patek",
                            "quantity": 1,
                            "unitPrice": 50000.00
                        }
                    ]
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Email", userEmail)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Insufficient stock")));
    }

    @Test
    void createOrder_secondItemFails_rollsBackFirstReservation() throws Exception {
        UUID product1 = UUID.randomUUID();
        UUID product2 = UUID.randomUUID();

        when(catalogClient.reserveStock(eq(product1), anyInt())).thenReturn(true);
        when(catalogClient.reserveStock(eq(product2), anyInt())).thenReturn(false);

        String body = """
                {
                    "items": [
                        {
                            "productId": "%s",
                            "productName": "Watch A",
                            "quantity": 1,
                            "unitPrice": 5000.00
                        },
                        {
                            "productId": "%s",
                            "productName": "Watch B",
                            "quantity": 1,
                            "unitPrice": 7000.00
                        }
                    ]
                }
                """.formatted(product1, product2);

        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Email", userEmail)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(catalogClient).releaseStock(eq(product1), eq(1));
    }

    @Test
    void createOrder_emptyItems_returnsBadRequest() throws Exception {
        String body = """
                {
                    "items": []
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Email", userEmail)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserOrders_returnsUserOrders() throws Exception {
        Order order = createTestOrder(userId, userEmail, OrderStatus.PENDING);
        orderRepository.save(order);

        mockMvc.perform(get("/api/orders")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(userId.toString()));
    }

    @Test
    void getUserOrders_otherUser_returnsEmpty() throws Exception {
        Order order = createTestOrder(userId, userEmail, OrderStatus.PENDING);
        orderRepository.save(order);

        UUID otherUserId = UUID.randomUUID();
        mockMvc.perform(get("/api/orders")
                        .header("X-User-Id", otherUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getOrderById_withUserId_success() throws Exception {
        Order order = createTestOrder(userId, userEmail, OrderStatus.PENDING);
        order = orderRepository.save(order);

        mockMvc.perform(get("/api/orders/{id}", order.getId())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getOrderById_wrongUser_returns404() throws Exception {
        Order order = createTestOrder(userId, userEmail, OrderStatus.PENDING);
        order = orderRepository.save(order);

        UUID otherUserId = UUID.randomUUID();
        mockMvc.perform(get("/api/orders/{id}", order.getId())
                        .header("X-User-Id", otherUserId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrderById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", UUID.randomUUID())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateOrderStatus_success() throws Exception {
        Order order = createTestOrder(userId, userEmail, OrderStatus.PENDING);
        order = orderRepository.save(order);

        String body = """
                {
                    "status": "PAID",
                    "paymentId": "PAY-12345",
                    "paymentStatus": "APPROVED"
                }
                """;

        mockMvc.perform(put("/api/orders/{id}/status", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentId").value("PAY-12345"))
                .andExpect(jsonPath("$.paymentStatus").value("APPROVED"));
    }

    @Test
    void cancelOrder_pendingOrder_success() throws Exception {
        when(catalogClient.releaseStock(any(UUID.class), anyInt())).thenReturn(true);

        Order order = createTestOrder(userId, userEmail, OrderStatus.PENDING);
        order = orderRepository.save(order);

        mockMvc.perform(post("/api/orders/{id}/cancel", order.getId())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(catalogClient).releaseStock(any(UUID.class), anyInt());
    }

    @Test
    void cancelOrder_shippedOrder_returnsBadRequest() throws Exception {
        Order order = createTestOrder(userId, userEmail, OrderStatus.SHIPPED);
        order = orderRepository.save(order);

        mockMvc.perform(post("/api/orders/{id}/cancel", order.getId())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Cannot cancel")));
    }

    @Test
    void cancelOrder_wrongUser_returnsBadRequest() throws Exception {
        Order order = createTestOrder(userId, userEmail, OrderStatus.PENDING);
        order = orderRepository.save(order);

        UUID otherUserId = UUID.randomUUID();
        mockMvc.perform(post("/api/orders/{id}/cancel", order.getId())
                        .header("X-User-Id", otherUserId.toString()))
                .andExpect(status().isBadRequest());
    }

    private Order createTestOrder(UUID userId, String email, OrderStatus status) {
        Order order = new Order();
        order.setUserId(userId);
        order.setUserEmail(email);
        order.setStatus(status);
        order.setCurrency("USD");
        order.setShippingAddress("Test Address");

        OrderItem item = new OrderItem();
        item.setProductId(UUID.randomUUID());
        item.setProductName("Test Watch");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("9999.99"));
        item.setTotalPrice(new BigDecimal("9999.99"));

        order.addItem(item);
        order.setSubtotal(new BigDecimal("9999.99"));
        order.setTax(BigDecimal.ZERO);
        order.setTotal(new BigDecimal("9999.99"));

        return order;
    }
}
