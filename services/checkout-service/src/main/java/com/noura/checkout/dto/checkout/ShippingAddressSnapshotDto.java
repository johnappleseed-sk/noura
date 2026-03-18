package com.noura.checkout.dto.checkout;

/**
 * Shipping address snapshot used in checkout preview/validation/order payloads.
 *
 * @param fullName recipient full name
 * @param phone recipient phone number
 * @param line1 address line 1
 * @param line2 address line 2
 * @param district district
 * @param city city
 * @param stateProvince state or province
 * @param postalCode postal code
 * @param countryCode country code
 * @param formattedAddress optional fully formatted address
 * @param validationStatus address validation status
 */
public record ShippingAddressSnapshotDto(
        String fullName,
        String phone,
        String line1,
        String line2,
        String district,
        String city,
        String stateProvince,
        String postalCode,
        String countryCode,
        String formattedAddress,
        String validationStatus
) {
}

