package com.noura.platform.dto.order;

import com.noura.platform.domain.enums.OrderStatus;
import com.noura.platform.domain.enums.RefundStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderTimelineEventDto(
        UUID id,
        UUID orderId,
        OrderStatus status,
        RefundStatus refundStatus,
        String actor,
        String note,
        Instant createdAt
) {
}

