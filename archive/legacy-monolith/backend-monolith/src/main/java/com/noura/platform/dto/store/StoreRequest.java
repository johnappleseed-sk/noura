package com.noura.platform.dto.store;

import com.noura.platform.domain.enums.StoreServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;

public record StoreRequest(
        @NotBlank String name,
        @NotBlank String addressLine1,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String zipCode,
        @NotBlank String country,
        @NotBlank String region,
        @NotNull BigDecimal latitude,
        @NotNull BigDecimal longitude,
        Integer serviceRadiusMeters,
        @NotNull LocalTime openTime,
        @NotNull LocalTime closeTime,
        boolean active,
        @NotEmpty Set<StoreServiceType> services,
        @NotNull BigDecimal shippingFee,
        @NotNull BigDecimal freeShippingThreshold
) {
}
