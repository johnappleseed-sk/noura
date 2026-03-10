package com.noura.platform.dto.merchandising;

import java.time.Instant;
import java.util.UUID;

public record MerchandisingBoostDto(
        UUID id,
        UUID productId,
        String productName,
        String label,
        double boostValue,
        boolean active,
        Instant startAt,
        Instant endAt
) {
}
