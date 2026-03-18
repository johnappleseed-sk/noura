package com.noura.order.dto.order;

import com.noura.order.domain.enums.OrderStatus;
import com.noura.order.domain.enums.RefundStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Order status timeline response DTO.
 *
 * @param id event identifier
 * @param orderId order identifier
 * @param status compatibility field equal to {@code toStatus}
 * @param fromStatus previous status
 * @param toStatus next status
 * @param refundStatus refund status at transition time
 * @param actor actor who changed the status
 * @param reason transition reason
 * @param note transition note
 * @param createdAt event timestamp
 */
public record OrderStatusEventResponse(
        UUID id,
        UUID orderId,
        OrderStatus status,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        RefundStatus refundStatus,
        String actor,
        String reason,
        String note,
        Instant createdAt
) {
}

