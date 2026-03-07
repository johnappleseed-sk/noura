package com.noura.platform.dto.user;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
        String label,
        @NotBlank String fullName,
        @NotBlank String line1,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String zipCode,
        @NotBlank String country,
        boolean defaultAddress
) {
}
