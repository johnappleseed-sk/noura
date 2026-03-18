package com.noura.catalog.dto.admin;

import java.time.Instant;
import java.util.UUID;

/**
 * Manual merchandising boost snapshot returned to admin-web.
 */
public record MerchandisingBoostResponse(
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
