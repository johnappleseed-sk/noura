package com.noura.platform.dto.analytics;

import com.noura.platform.domain.enums.AnalyticsEventType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AnalyticsEventDto(
        UUID id,
        AnalyticsEventType eventType,
        String sessionId,
        String customerRef,
        String productId,
        String orderId,
        String promotionCode,
        String storeId,
        String channelId,
        String locale,
        String pagePath,
        String source,
        Instant occurredAt,
        Map<String, Object> metadata
) {
}
