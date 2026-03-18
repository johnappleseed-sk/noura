package com.noura.order.dto.order;

/**
 * Immutable address snapshot payload copied into order records.
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
 */
public record AddressSnapshotDto(
        String fullName,
        String phone,
        String line1,
        String line2,
        String district,
        String city,
        String stateProvince,
        String postalCode,
        String countryCode
) {
}

