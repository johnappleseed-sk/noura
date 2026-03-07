package com.noura.platform.dto.pricing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PriceUpsertRequest(
        @NotNull UUID variantId,
        @NotNull UUID priceListId,
        @NotNull BigDecimal amount,
        @NotBlank String currency,
        Instant startDate,
        Instant endDate,
        Integer priority
) {
}
