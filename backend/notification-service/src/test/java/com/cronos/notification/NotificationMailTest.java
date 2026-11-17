package com.cronos.notification;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** With MAIL_HOST set the context still starts (single mail sender) and a real SMTP conversation takes place. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationMailTest {

    static final List<String> delivered = new CopyOnWriteArrayList<>();
    static volatile boolean rejectRecipients;
    static final ServerSocket smtp = startSmtp();

    static ServerSocket startSmtp() {
        try {
            ServerSocket server = new ServerSocket(0);
            Thread thread = new Thread(() -> {
                while (!server.isClosed()) {
                    try (Socket socket = server.accept();
                         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                         PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
                        out.print("220 test ESMTP\r\n"); out.flush();
                        StringBuilder message = new StringBuilder();
                        boolean data = false;
                        String line;
                        while ((line = in.readLine()) != null) {
                            if (data) {
                                if (line.equals(".")) { data = false; delivered.add(message.toString()); out.print("250 queued\r\n"); }
                                else message.append(line).append('\n');
                            } else if (line.startsWith("EHLO") || line.startsWith("HELO")) out.print("250 test\r\n");
                            else if (line.startsWith("RCPT") && rejectRecipients) out.print("550 mailbox unavailable\r\n");
                            else if (line.startsWith("DATA")) { data = true; out.print("354 go ahead\r\n"); }
                            else if (line.startsWith("QUIT")) { out.print("221 bye\r\n"); out.flush(); break; }
                            else out.print("250 ok\r\n");
                            out.flush();
                        }
                    } catch (IOException ignored) {
                        // socket closed
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void mail(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> "127.0.0.1");
        registry.add("spring.mail.port", smtp::getLocalPort);
        registry.add("spring.mail.properties.mail.smtp.auth", () -> "false");
        registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
    }

    @AfterAll
    static void stop() throws IOException {
        smtp.close();
    }

    @Autowired MockMvc mvc;

    @Test
    void emailIsReallySent_thenFailureIsRecordedHonestly() throws Exception {
        String body = "{\"userId\":\"" + UUID.randomUUID() + "\",\"userEmail\":\"buyer@cronos.test\",\"type\":\"ORDER_CREATED\","
                + "\"subject\":\"We received your order\",\"body\":\"Thank you for your order.\"}";
        mvc.perform(post("/internal/notifications").header("X-Internal-Token", NotificationApiTest.TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SENT")).andExpect(jsonPath("$.sentAt").exists());
        assertThat(delivered).hasSize(1);
        assertThat(delivered.get(0)).contains("Subject: We received your order").contains("To: buyer@cronos.test")
                .contains("Thank you for your order.");

        rejectRecipients = true;
        mvc.perform(post("/internal/notifications").header("X-Internal-Token", NotificationApiTest.TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FAILED")).andExpect(jsonPath("$.error").exists());
        assertThat(delivered).hasSize(1);
    }
}
