package com.cronos.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SendNotificationRequest(
        @NotNull UUID userId,
        @NotBlank @Email String userEmail,
        @NotBlank String type,
        @NotBlank String subject,
        @NotBlank String body,
        UUID orderId
) {}
