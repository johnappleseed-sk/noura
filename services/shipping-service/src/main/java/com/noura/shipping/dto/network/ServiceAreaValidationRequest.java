package com.noura.shipping.dto.network;

import com.noura.shipping.domain.enums.StoreServiceType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Coordinate validation payload aligned with admin-web service-area tooling.
 */
public record ServiceAreaValidationRequest(
        @NotNull BigDecimal latitude,
        @NotNull BigDecimal longitude,
        @NotNull StoreServiceType serviceType,
        Instant at,
        Long maxDistanceMeters
) {
}
