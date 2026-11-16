package com.cronos.payment;

import com.cronos.payment.client.MercadoPagoClient;
import com.cronos.payment.client.OrderClient;
import com.cronos.payment.config.MercadoPagoConfig;
import com.cronos.payment.entity.Payment;
import com.cronos.payment.entity.PaymentStatus;
import com.cronos.payment.repository.PaymentRepository;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MercadoPagoConfig mercadoPagoConfig;

    @MockBean
    private MercadoPagoClient mercadoPagoClient;

    @MockBean
    private OrderClient orderClient;

    private static final String WEBHOOK_SECRET = "test-webhook-secret-key";

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    void createPreference_success() throws Exception {
        UUID orderId = UUID.randomUUID();

        when(mercadoPagoClient.createPreference(anyString(), any(), anyString(), anyString()))
                .thenReturn(Optional.of(new MercadoPagoClient.PreferenceResult(
                        "PREF-123", "https://mp.com/init", "https://sandbox.mp.com/init")));

        String body = """
                {
                    "orderId": "%s",
                    "items": [
                        {"title": "Rolex Submariner", "quantity": 1, "unitPrice": 12500.00}
                    ],
                    "payerEmail": "buyer@test.com",
                    "totalAmount": 12500.00,
                    "currency": "USD"
                }
                """.formatted(orderId);

        mockMvc.perform(post("/api/payments/preference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferenceId").value("PREF-123"))
                .andExpect(jsonPath("$.initPoint").value("https://mp.com/init"))
                .andExpect(jsonPath("$.sandboxInitPoint").value("https://sandbox.mp.com/init"));
    }

    @Test
    void createPreference_mpNotConfigured_returnNulls() throws Exception {
        UUID orderId = UUID.randomUUID();

        when(mercadoPagoClient.createPreference(anyString(), any(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        String body = """
                {
                    "orderId": "%s",
                    "items": [
                        {"title": "Watch", "quantity": 1, "unitPrice": 5000.00}
                    ],
                    "payerEmail": "test@test.com",
                    "totalAmount": 5000.00,
                    "currency": "USD"
                }
                """.formatted(orderId);

        mockMvc.perform(post("/api/payments/preference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferenceId").isEmpty());

        assertEquals(1, paymentRepository.findByOrderId(orderId).size());
    }

    @Test
    void webhook_missingSignature_returns401() throws Exception {
        String body = """
                {
                    "type": "payment",
                    "data": {"id": "12345"}
                }
                """;

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhook_invalidSignature_returns401() throws Exception {
        String body = """
                {
                    "type": "payment",
                    "data": {"id": "12345"}
                }
                """;

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("x-signature", "ts=1234567890,v1=invalid_signature_value")
                        .header("x-request-id", "req-abc"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhook_validSignature_processesPayment() throws Exception {
        UUID orderId = UUID.randomUUID();
        String paymentId = "98765";
        String requestId = "req-xyz-123";
        String ts = "1700000000";

        Payment existingPayment = new Payment();
        existingPayment.setOrderId(orderId);
        existingPayment.setAmount(new BigDecimal("12500.00"));
        existingPayment.setCurrency("USD");
        existingPayment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(existingPayment);

        when(mercadoPagoClient.getPayment(paymentId))
                .thenReturn(Optional.of(new MercadoPagoClient.PaymentInfo(
                        paymentId, "approved", "accredited",
                        new BigDecimal("12500.00"), "USD",
                        orderId.toString(), "credit_card", "buyer@test.com")));
        when(orderClient.updateOrderStatus(any(UUID.class), anyString())).thenReturn(true);

        String signature = computeSignature(paymentId, requestId, ts, WEBHOOK_SECRET);

        String body = """
                {
                    "type": "payment",
                    "data": {"id": "%s"}
                }
                """.formatted(paymentId);

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("x-signature", "ts=" + ts + ",v1=" + signature)
                        .header("x-request-id", requestId))
                .andExpect(status().isOk());

        verify(mercadoPagoClient).getPayment(paymentId);
        verify(orderClient).updateOrderStatus(orderId, "PAID");

        Payment updated = paymentRepository.findByOrderId(orderId).getFirst();
        assertEquals(PaymentStatus.APPROVED, updated.getStatus());
        assertNotNull(updated.getVerifiedAt());
        assertEquals(paymentId, updated.getExternalPaymentId());
    }

    @Test
    void webhook_duplicatePayment_isIdempotent() throws Exception {
        UUID orderId = UUID.randomUUID();
        String paymentId = "55555";
        String requestId = "req-dup";
        String ts = "1700000000";

        Payment existing = new Payment();
        existing.setOrderId(orderId);
        existing.setAmount(new BigDecimal("5000.00"));
        existing.setCurrency("USD");
        existing.setStatus(PaymentStatus.APPROVED);
        existing.setIdempotencyKey("webhook:" + paymentId);
        paymentRepository.save(existing);

        String signature = computeSignature(paymentId, requestId, ts, WEBHOOK_SECRET);

        String body = """
                {
                    "type": "payment",
                    "data": {"id": "%s"}
                }
                """.formatted(paymentId);

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("x-signature", "ts=" + ts + ",v1=" + signature)
                        .header("x-request-id", requestId))
                .andExpect(status().isOk());

        verifyNoInteractions(mercadoPagoClient);
        verifyNoInteractions(orderClient);
    }

    @Test
    void webhook_nonPaymentType_isIgnored() throws Exception {
        String body = """
                {
                    "type": "merchant_order",
                    "data": {"id": "99999"}
                }
                """;

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verifyNoInteractions(mercadoPagoClient);
    }

    @Test
    void webhook_amountMismatch_doesNotUpdateOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        String paymentId = "77777";
        String requestId = "req-mismatch";
        String ts = "1700000000";

        Payment existingPayment = new Payment();
        existingPayment.setOrderId(orderId);
        existingPayment.setAmount(new BigDecimal("12500.00"));
        existingPayment.setCurrency("USD");
        existingPayment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(existingPayment);

        when(mercadoPagoClient.getPayment(paymentId))
                .thenReturn(Optional.of(new MercadoPagoClient.PaymentInfo(
                        paymentId, "approved", "accredited",
                        new BigDecimal("1.00"), "USD",
                        orderId.toString(), "credit_card", "hacker@evil.com")));

        String signature = computeSignature(paymentId, requestId, ts, WEBHOOK_SECRET);

        String body = """
                {
                    "type": "payment",
                    "data": {"id": "%s"}
                }
                """.formatted(paymentId);

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("x-signature", "ts=" + ts + ",v1=" + signature)
                        .header("x-request-id", requestId))
                .andExpect(status().isOk());

        verifyNoInteractions(orderClient);

        Payment unchanged = paymentRepository.findByOrderId(orderId).getFirst();
        assertEquals(PaymentStatus.PENDING, unchanged.getStatus());
    }

    @Test
    void webhook_rejectedPayment_updatesOrderCorrectly() throws Exception {
        UUID orderId = UUID.randomUUID();
        String paymentId = "88888";
        String requestId = "req-rejected";
        String ts = "1700000000";

        Payment existingPayment = new Payment();
        existingPayment.setOrderId(orderId);
        existingPayment.setAmount(new BigDecimal("8000.00"));
        existingPayment.setCurrency("USD");
        existingPayment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(existingPayment);

        when(mercadoPagoClient.getPayment(paymentId))
                .thenReturn(Optional.of(new MercadoPagoClient.PaymentInfo(
                        paymentId, "rejected", "cc_rejected_other_reason",
                        new BigDecimal("8000.00"), "USD",
                        orderId.toString(), "credit_card", "buyer@test.com")));
        when(orderClient.updateOrderStatus(any(UUID.class), anyString())).thenReturn(true);

        String signature = computeSignature(paymentId, requestId, ts, WEBHOOK_SECRET);

        String body = """
                {
                    "type": "payment",
                    "data": {"id": "%s"}
                }
                """.formatted(paymentId);

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("x-signature", "ts=" + ts + ",v1=" + signature)
                        .header("x-request-id", requestId))
                .andExpect(status().isOk());

        verify(orderClient).updateOrderStatus(orderId, "PAYMENT_FAILED");

        Payment updated = paymentRepository.findByOrderId(orderId).getFirst();
        assertEquals(PaymentStatus.REJECTED, updated.getStatus());
        assertNull(updated.getVerifiedAt());
    }

    @Test
    void getPaymentsByOrder_returnsPayments() throws Exception {
        UUID orderId = UUID.randomUUID();

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(new BigDecimal("15000.00"));
        payment.setCurrency("USD");
        payment.setStatus(PaymentStatus.APPROVED);
        payment.setExternalPaymentId("EXT-123");
        paymentRepository.save(payment);

        mockMvc.perform(get("/api/payments/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].orderId").value(orderId.toString()))
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }

    @Test
    void getPaymentById_found() throws Exception {
        Payment payment = new Payment();
        payment.setOrderId(UUID.randomUUID());
        payment.setAmount(new BigDecimal("7500.00"));
        payment.setCurrency("USD");
        payment.setStatus(PaymentStatus.PENDING);
        payment = paymentRepository.save(payment);

        mockMvc.perform(get("/api/payments/{id}", payment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payment.getId().toString()))
                .andExpect(jsonPath("$.amount").value(7500.00));
    }

    @Test
    void getPaymentById_notFound() throws Exception {
        mockMvc.perform(get("/api/payments/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private String computeSignature(String dataId, String requestId, String ts, String secret) {
        String manifest = String.format("id:%s;request-id:%s;ts:%s;", dataId, requestId, ts);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
