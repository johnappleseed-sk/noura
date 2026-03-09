package com.noura.platform.dto.inventory;

import java.math.BigDecimal;

public record VariantUnitDeductionResultDto(
        Long variantId,
        Long sellUnitId,
        BigDecimal soldQty,
        BigDecimal deductedBaseQty,
        BigDecimal remainingBaseQty
) {
}
