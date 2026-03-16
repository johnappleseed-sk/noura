package com.noura.platform.dto.pricing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PriceDto(
        UUID id,
        UUID variantId,
        UUID priceListId,
        BigDecimal amount,
        String currency,
        Instant startDate,
        Instant endDate,
        int priority
) {
}
