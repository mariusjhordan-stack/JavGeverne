package com.cronos.notification.controller;

import com.cronos.notification.dto.NotificationResponse;
import com.cronos.notification.dto.SendNotificationRequest;
import com.cronos.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {
        return ResponseEntity.ok(notificationService.sendNotification(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponse>> getByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(notificationService.getNotificationsByUserId(userId));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<NotificationResponse>> getByOrderId(@PathVariable UUID orderId) {
        return ResponseEntity.ok(notificationService.getNotificationsByOrderId(orderId));
    }
}
