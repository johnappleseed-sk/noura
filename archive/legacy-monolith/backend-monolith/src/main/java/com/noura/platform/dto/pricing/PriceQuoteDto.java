package com.noura.platform.dto.pricing;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PriceQuoteDto(
        UUID variantId,
        String currency,
        BigDecimal baseAmount,
        BigDecimal finalAmount,
        List<UUID> appliedPromotionIds
) {
}
