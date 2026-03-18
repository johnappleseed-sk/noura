package com.noura.order.dto.order;

import com.noura.order.domain.enums.OrderStatus;
import com.noura.order.domain.enums.RefundStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Order status update payload for admin/internal transitions.
 *
 * @param status target order status
 * @param refundStatus target refund status
 * @param reason optional reason
 * @param note optional note
 */
public record UpdateOrderStatusRequest(
        @NotNull OrderStatus status,
        @NotNull RefundStatus refundStatus,
        @Size(max = 255) String reason,
        @Size(max = 600) String note
) {
}

