package com.noura.platform.inventory.dto.webhook;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WebhookSubscriptionRequest(
        @NotBlank @Size(max = 120) String eventCode,
        @NotBlank @Size(max = 1000) String endpointUrl,
        @Size(max = 255) String secretToken,
        Boolean active,
        @Min(1000) @Max(60000) Integer timeoutMs,
        @Min(0) @Max(10) Integer retryCount
) {
}
