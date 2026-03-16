package com.noura.platform.dto.storefront;

import java.math.BigDecimal;

public record StorefrontCartItemDto(
        Long id,
        Long productId,
        String sku,
        String productName,
        String unitLabel,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
