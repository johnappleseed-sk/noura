package com.noura.shipping.dto.shipping;

import jakarta.validation.constraints.NotBlank;

/**
 * Structured address payload used by quote and shipment APIs.
 *
 * @param fullName recipient full name
 * @param phone recipient phone
 * @param line1 address line 1
 * @param line2 address line 2
 * @param district district
 * @param city city
 * @param stateProvince state or province
 * @param postalCode postal code
 * @param countryCode ISO-like country code
 */
public record AddressRequest(
        String fullName,
        String phone,
        String line1,
        String line2,
        String district,
        String city,
        String stateProvince,
        String postalCode,
        @NotBlank(message = "countryCode is required")
        String countryCode
) {
}
