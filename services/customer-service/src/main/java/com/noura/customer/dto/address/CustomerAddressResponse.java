package com.noura.customer.dto.address;

import com.noura.customer.domain.enums.AddressValidationStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Customer address response DTO.
 *
 * @param id address identifier
 * @param label address label
 * @param fullName recipient full name
 * @param phone recipient phone
 * @param line1 address line 1
 * @param line2 address line 2
 * @param district district
 * @param city city
 * @param stateProvince state or province
 * @param postalCode postal code
 * @param countryCode country code
 * @param latitude latitude
 * @param longitude longitude
 * @param accuracyMeters geolocation accuracy
 * @param placeId place ID
 * @param formattedAddress formatted address
 * @param deliveryInstructions delivery notes
 * @param validationStatus address validation status
 * @param defaultShipping default shipping flag
 * @param defaultBilling default billing flag
 * @param defaultAddress compatibility flag representing either shipping or billing default
 */
public record CustomerAddressResponse(
        UUID id,
        String label,
        String fullName,
        String phone,
        String line1,
        String line2,
        String district,
        String city,
        String stateProvince,
        String postalCode,
        String countryCode,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer accuracyMeters,
        String placeId,
        String formattedAddress,
        String deliveryInstructions,
        AddressValidationStatus validationStatus,
        boolean defaultShipping,
        boolean defaultBilling,
        boolean defaultAddress
) {
}
