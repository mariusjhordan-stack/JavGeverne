package com.cronos.auth.dto;

public record AuthResponse(
        String token,
        String refreshToken,
        String email,
        String role,
        String firstName,
        String lastName
) {}
