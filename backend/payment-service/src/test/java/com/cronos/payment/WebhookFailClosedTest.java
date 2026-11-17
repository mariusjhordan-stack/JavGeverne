package com.cronos.payment;

import com.cronos.payment.repository.PaymentEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** With no webhook secret and no access token the service starts, but refuses webhooks and checkout. */
@SpringBootTest(properties = {"mercadopago.webhook-secret=", "mercadopago.access-token="})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebhookFailClosedTest {

    @Autowired MockMvc mvc;
    @Autowired PaymentEventRepository events;

    @Test
    void webhooksAreRefusedWhenNoSecretIsConfigured() throws Exception {
        mvc.perform(post("/api/payments/webhook").param("data.id", "123").param("type", "payment")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"payment\",\"data\":{\"id\":\"123\"}}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("WEBHOOK_NOT_CONFIGURED"));
        mvc.perform(post("/api/payments/webhook").param("data.id", "123").param("type", "payment")
                        .header("x-signature", "ts=1,v1=00").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isServiceUnavailable());
        assertThat(events.count()).isZero();
    }

    @Test
    void checkoutSaysClearlyThatPaymentIsUnavailable() throws Exception {
        mvc.perform(post("/api/payments/preference").header("X-Internal-Token", "test-internal-token-0123456789abcdef0123")
                        .header("X-User-Id", UUID.randomUUID().toString()).header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"orderId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("PAYMENT_PROVIDER_NOT_CONFIGURED"));
    }
}
