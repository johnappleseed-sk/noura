package com.noura.platform.dto.pricing;

import java.math.BigDecimal;

public record SkuSellUnitDto(
        Long id,
        Long variantId,
        Long unitId,
        String unitCode,
        BigDecimal conversionToBase,
        Boolean isBase,
        BigDecimal basePrice,
        Boolean enabled,
        Long version
) {
}
