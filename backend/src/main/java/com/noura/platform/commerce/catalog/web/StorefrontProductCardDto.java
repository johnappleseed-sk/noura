package com.noura.platform.commerce.catalog.web;

import java.math.BigDecimal;

public record StorefrontProductCardDto(
        Long id,
        String sku,
        String name,
        BigDecimal price,
        String imageUrl,
        Long categoryId,
        String categoryName,
        Integer stockQty,
        boolean lowStock,
        boolean allowNegativeStock
) {
}
