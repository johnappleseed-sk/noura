package com.noura.platform.dto.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SkuUnitTierPriceDto(
        Long id,
        Long sellUnitId,
        Long customerGroupId,
        String customerGroupCode,
        BigDecimal minQty,
        BigDecimal unitPrice,
        String currencyCode,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        Boolean active
) {
}
