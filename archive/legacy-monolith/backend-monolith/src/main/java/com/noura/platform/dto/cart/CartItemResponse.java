package com.noura.platform.dto.cart;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID storeProductReferenceId,
        UUID productId,
        String productCode,
        String productName,
        UUID storeId,
        String storeName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        Instant createdAt,
        Instant updatedAt
) {
}
