package com.cronos.auth.dto;

import java.time.LocalDateTime;

public record AdminUserResponse(
        String id,
        String email,
        String firstName,
        String lastName,
        String role,
        boolean enabled,
        LocalDateTime createdAt
) {}
