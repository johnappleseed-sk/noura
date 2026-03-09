package com.noura.platform.dto.storefront;

import java.math.BigDecimal;

public record StorefrontOrderItemDto(
        Long id,
        Long productId,
        String sku,
        String productName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
