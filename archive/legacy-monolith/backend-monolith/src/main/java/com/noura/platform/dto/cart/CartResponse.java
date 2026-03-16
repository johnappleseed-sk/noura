package com.noura.platform.dto.cart;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        UUID userId,
        UUID storeId,
        List<CartItemResponse> items,
        int totalQuantity,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal shippingAmount,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant updatedAt
) {
}
