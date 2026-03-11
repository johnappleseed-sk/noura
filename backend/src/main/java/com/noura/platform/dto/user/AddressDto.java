package com.noura.platform.dto.user;

import com.noura.platform.domain.enums.AddressValidationStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record AddressDto(
        UUID id,
        String label,
        String fullName,
        String phone,
        String line1,
        String line2,
        String district,
        String city,
        String state,
        String zipCode,
        String country,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer accuracyMeters,
        String placeId,
        String formattedAddress,
        String deliveryInstructions,
        AddressValidationStatus validationStatus,
        boolean defaultAddress
) {
}
