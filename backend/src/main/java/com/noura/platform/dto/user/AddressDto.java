package com.noura.platform.dto.user;

import java.util.UUID;

public record AddressDto(
        UUID id,
        String label,
        String fullName,
        String line1,
        String city,
        String state,
        String zipCode,
        String country,
        boolean defaultAddress
) {
}
