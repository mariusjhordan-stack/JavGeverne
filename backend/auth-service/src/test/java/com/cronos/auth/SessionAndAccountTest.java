package com.cronos.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionAndAccountTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    JsonNode register() throws Exception {
        String email = "s" + UUID.randomUUID() + "@cronos.test";
        String body = "{\"email\":\"" + email + "\",\"password\":\"Passw0rd!123\",\"firstName\":\"Sam\",\"lastName\":\"Lee\"}";
        return json.readTree(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    String refreshBody(String token) {
        return "{\"refreshToken\":\"" + token + "\"}";
    }

    @Test
    void refreshTokensAreSingleUse_andReuseEndsTheSession() throws Exception {
        String first = register().get("refreshToken").asText();
        String second = json.readTree(mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(first)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("refreshToken").asText();
        // The first token was exchanged: using it again is treated as theft and kills the newer token too.
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(first))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(second))).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {
        String refresh = register().get("refreshToken").asText();
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON).content(refreshBody(refresh))).andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(refresh))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isNoContent());
    }

    @Test
    void profileAndPasswordCanBeChanged() throws Exception {
        JsonNode session = register();
        String auth = "Bearer " + session.get("token").asText();
        mvc.perform(put("/api/auth/profile").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Samuel\",\"lastName\":\"Lee\",\"phone\":\"+51 999 111 222\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.firstName").value("Samuel"))
                .andExpect(jsonPath("$.phone").value("+51 999 111 222")).andExpect(jsonPath("$.role").value("USER"));
        mvc.perform(put("/api/auth/password").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"NewPassw0rd!\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/auth/password").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Passw0rd!123\",\"newPassword\":\"NewPassw0rd!\"}")).andExpect(status().isNoContent());
        String email = session.get("email").asText();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Passw0rd!123\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"NewPassw0rd!\"}")).andExpect(status().isOk());
        // Changing the password signs out other sessions.
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(session.get("refreshToken").asText()))).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/auth/profile").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
    }

    @Test
    void addressesBelongToTheirOwner() throws Exception {
        String alice = "Bearer " + register().get("token").asText();
        String bob = "Bearer " + register().get("token").asText();
        String address = "{\"recipient\":\"Alice A\",\"line1\":\"Av. Larco 123\",\"city\":\"Lima\",\"country\":\"Peru\"}";
        String id = json.readTree(mvc.perform(post("/api/auth/addresses").header("Authorization", alice).contentType(MediaType.APPLICATION_JSON).content(address))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.defaultAddress").value(true))
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        mvc.perform(get("/api/auth/addresses").header("Authorization", alice)).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/auth/addresses").header("Authorization", bob)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(put("/api/auth/addresses/" + id).header("Authorization", bob).contentType(MediaType.APPLICATION_JSON).content(address))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/auth/addresses/" + id).header("Authorization", bob)).andExpect(status().isNotFound());
        mvc.perform(post("/api/auth/addresses").header("Authorization", alice).contentType(MediaType.APPLICATION_JSON).content("{\"city\":\"Lima\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/auth/addresses/" + id).header("Authorization", alice)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/addresses").header("Authorization", alice)).andExpect(jsonPath("$", hasSize(0)));
    }
}
