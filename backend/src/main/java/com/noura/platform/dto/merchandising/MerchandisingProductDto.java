package com.noura.platform.dto.merchandising;

import java.math.BigDecimal;
import java.util.UUID;

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
