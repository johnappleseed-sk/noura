package com.noura.platform.dto.order;

import com.noura.platform.domain.enums.FulfillmentMethod;
import com.noura.platform.domain.enums.OrderStatus;
import com.noura.platform.domain.enums.RefundStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDto(
        UUID id,
        UUID userId,
        UUID storeId,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal shippingAmount,
        BigDecimal totalAmount,
        FulfillmentMethod fulfillmentMethod,
        OrderStatus status,
        RefundStatus refundStatus,
        String couponCode,
        Instant createdAt,
        List<OrderItemDto> items
) {
}
