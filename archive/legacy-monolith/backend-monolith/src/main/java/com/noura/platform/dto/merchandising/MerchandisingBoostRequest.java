package com.noura.platform.dto.merchandising;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record MerchandisingBoostRequest(
        @NotNull UUID productId,
        @NotBlank @Size(max = 120) String label,
        @NotNull @DecimalMin("0.0") Double boostValue,
        Boolean active,
        Instant startAt,
        Instant endAt
) {
}
