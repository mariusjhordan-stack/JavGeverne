package com.cronos.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Default configuration (no MAIL_HOST): the context must start and notifications are recorded as SKIPPED. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationApiTest {

    static final String TOKEN = "test-internal-token-0123456789abcdef0123";

    @Autowired MockMvc mvc;

    static String body(UUID user, String type) {
        return "{\"userId\":\"" + user + "\",\"userEmail\":\"buyer@cronos.test\",\"type\":\"" + type
                + "\",\"subject\":\"Subject\",\"body\":\"Body\",\"orderId\":\"" + UUID.randomUUID() + "\"}";
    }

    MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder b, UUID user, String role) {
        return b.header("X-Internal-Token", TOKEN).header("X-User-Id", user.toString()).header("X-User-Role", role)
                .header("X-User-Email", "buyer@cronos.test");
    }

    @Test
    void servicesCanRecordNotifications_statusIsHonestWhenMailIsOff() throws Exception {
        UUID user = UUID.randomUUID();
        mvc.perform(post("/internal/notifications").header("X-Internal-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(body(user, "PAYMENT_APPROVED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SKIPPED"))
                .andExpect(jsonPath("$.sentAt").doesNotExist());
        mvc.perform(as(get("/api/notifications"), user, "USER"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("PAYMENT_APPROVED"));
        mvc.perform(as(get("/api/notifications"), UUID.randomUUID(), "USER")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void onlyServicesMaySend() throws Exception {
        UUID user = UUID.randomUUID();
        mvc.perform(post("/internal/notifications").contentType(MediaType.APPLICATION_JSON).content(body(user, "ORDER_CREATED")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/notifications").header("X-Internal-Token", "wrong-wrong-wrong-wrong-wrong-wrong-1")
                        .header("X-User-Role", "ADMIN").header("X-User-Id", user.toString())
                        .contentType(MediaType.APPLICATION_JSON).content(body(user, "ORDER_CREATED")))
                .andExpect(status().isUnauthorized());
        // The old public endpoint is gone, even for an authenticated administrator.
        mvc.perform(as(post("/api/notifications"), user, "ADMIN").contentType(MediaType.APPLICATION_JSON).content(body(user, "ORDER_CREATED")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void invalidRequestsAreRejected() throws Exception {
        mvc.perform(post("/internal/notifications").header("X-Internal-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(body(UUID.randomUUID(), "NOPE")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/internal/notifications").header("X-Internal-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userEmail\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void readingRequiresIdentity_andAdminListIsAdminOnly() throws Exception {
        mvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/notifications").header("X-User-Id", UUID.randomUUID().toString()).header("X-User-Role", "USER"))
                .andExpect(status().isUnauthorized());
        mvc.perform(as(get("/api/admin/notifications"), UUID.randomUUID(), "USER")).andExpect(status().isForbidden());
        mvc.perform(as(get("/api/admin/notifications"), UUID.randomUUID(), "ADMIN")).andExpect(status().isOk());
    }
}
