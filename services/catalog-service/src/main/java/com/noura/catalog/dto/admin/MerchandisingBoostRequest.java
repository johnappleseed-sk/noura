package com.noura.catalog.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Manual merchandising boost command used by admin-web.
 */
public record MerchandisingBoostRequest(
        @NotNull UUID productId,
        @NotBlank String label,
        @DecimalMin("0.0") double boostValue,
        boolean active,
        Instant startAt,
        Instant endAt
) {
}
