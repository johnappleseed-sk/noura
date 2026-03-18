package com.noura.customer.dto.address;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * Address create/update command payload.
 *
 * @param label optional label
 * @param fullName recipient full name
 * @param phone recipient phone
 * @param line1 address line 1
 * @param line2 address line 2
 * @param district optional district
 * @param city city
 * @param stateProvince state or province
 * @param postalCode postal code
 * @param countryCode ISO-like country code
 * @param latitude optional latitude
 * @param longitude optional longitude
 * @param accuracyMeters optional geolocation accuracy
 * @param placeId optional provider place identifier
 * @param formattedAddress optional geocoder formatted address
 * @param deliveryInstructions optional delivery note
 * @param defaultShipping default shipping flag
 * @param defaultBilling default billing flag
 */
public record CustomerAddressRequest(
        String label,
        @NotBlank String fullName,
        String phone,
        @NotBlank String line1,
        String line2,
        String district,
        @NotBlank String city,
        @NotBlank String stateProvince,
        @NotBlank String postalCode,
        @NotBlank String countryCode,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        Integer accuracyMeters,
        String placeId,
        String formattedAddress,
        String deliveryInstructions,
        Boolean defaultShipping,
        Boolean defaultBilling
) {
}
