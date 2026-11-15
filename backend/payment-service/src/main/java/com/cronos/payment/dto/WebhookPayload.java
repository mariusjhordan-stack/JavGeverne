package com.cronos.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookPayload(
        String action,
        String type,
        @JsonProperty("data") WebhookData data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WebhookData(String id) {}
}
