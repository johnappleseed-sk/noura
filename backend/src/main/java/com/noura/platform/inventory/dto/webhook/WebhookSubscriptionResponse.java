package com.noura.platform.inventory.dto.webhook;

import java.time.Instant;

public record WebhookSubscriptionResponse(
        String id,
        String eventCode,
        String endpointUrl,
        boolean active,
        int timeoutMs,
        int retryCount,
        Instant createdAt,
        Instant updatedAt
) {
}
