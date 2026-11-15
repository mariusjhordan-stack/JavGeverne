package com.cronos.payment.dto;

public record PreferenceResponse(
        String preferenceId,
        String initPoint,
        String sandboxInitPoint
) {}
