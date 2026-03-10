package com.noura.platform.dto.user;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record AddressRequest(
        String label,
        String phone,
        @NotBlank String fullName,
        @NotBlank String line1,
        String line2,
        String district,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String zipCode,
        @NotBlank String country,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        Integer accuracyMeters,
        String placeId,
        String formattedAddress,
        String deliveryInstructions,
        boolean defaultAddress
) {
}
