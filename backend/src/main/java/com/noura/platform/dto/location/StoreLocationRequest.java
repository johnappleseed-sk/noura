package com.noura.platform.dto.location;

import com.noura.platform.domain.enums.StoreServiceType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;

public record StoreLocationRequest(
        @NotBlank String addressLine1,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String zipCode,
        @NotBlank String country,
        @NotBlank String region,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @Positive Integer serviceRadiusMeters,
        @NotNull LocalTime openTime,
        @NotNull LocalTime closeTime,
        boolean active,
        @NotEmpty Set<StoreServiceType> services
) {
}
