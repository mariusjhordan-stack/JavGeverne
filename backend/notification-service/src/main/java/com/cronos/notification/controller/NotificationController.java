package com.cronos.notification.controller;

import com.cronos.notification.security.RequestUser;
import com.cronos.notification.service.NotificationService;
import com.cronos.notification.service.NotificationService.View;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class NotificationController {

    public record SendRequest(@NotNull UUID userId, @NotBlank @Email String userEmail, @NotBlank String type,
                              @NotBlank @Size(max = 500) String subject, @NotBlank @Size(max = 10000) String body, UUID orderId) {}

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    /** Service-to-service only: never routed by the gateway and requires the internal token. */
    @PostMapping("/internal/notifications")
    public View send(@Valid @RequestBody SendRequest request) {
        return service.send(request.userId(), request.userEmail(), request.type(), request.subject(), request.body(), request.orderId());
    }

    @GetMapping("/api/notifications")
    public List<View> mine(@AuthenticationPrincipal RequestUser user) {
        return service.forUser(user.id());
    }

    @GetMapping("/api/admin/notifications")
    public Page<View> adminList(@RequestParam(required = false) UUID orderId,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "30") int size) {
        return service.adminList(orderId, page, size);
    }
}
