package com.cronos.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        String userEmail,
        String type,
        String subject,
        String body,
        String status,
        UUID orderId,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {}
