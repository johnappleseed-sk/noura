package com.noura.catalog.dto.admin;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Ranked merchandising preview product tailored to admin-web.
 */
public record MerchandisingProductDto(
        UUID id,
        String name,
        UUID categoryId,
        String categoryName,
        BigDecimal price,
        BigDecimal compareAtPrice,
        String imageUrl,
        int stockQty,
        boolean lowStock,
        boolean allowNegativeStock,
        boolean isNew,
        boolean isTrending,
        boolean isBestseller,
        double merchandisingScore
) {
}
