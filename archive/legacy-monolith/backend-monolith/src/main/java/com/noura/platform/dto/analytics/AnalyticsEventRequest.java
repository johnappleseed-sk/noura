package com.noura.platform.dto.analytics;

import com.noura.platform.domain.enums.AnalyticsEventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;

public record AnalyticsEventRequest(
        @NotNull AnalyticsEventType eventType,
        @Size(max = 120) String sessionId,
        @Size(max = 120) String customerRef,
        @Size(max = 120) String productId,
        @Size(max = 120) String orderId,
        @Size(max = 120) String promotionCode,
        @Size(max = 120) String storeId,
        @Size(max = 120) String channelId,
        @Size(max = 32) String locale,
        @Size(max = 255) String pagePath,
        @Size(max = 80) String source,
        Instant occurredAt,
        Map<String, Object> metadata
) {
}
