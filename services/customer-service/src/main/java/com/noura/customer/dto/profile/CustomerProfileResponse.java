package com.noura.customer.dto.profile;

import java.util.UUID;

/**
 * Customer profile response DTO.
 *
 * @param id customer profile identifier
 * @param externalSubject external identity subject key
 * @param fullName full name
 * @param email email address
 * @param phone phone number
 * @param enabled account active status
 * @param defaultShippingAddressId default shipping address identifier
 * @param defaultBillingAddressId default billing address identifier
 */
public record CustomerProfileResponse(
        UUID id,
        String externalSubject,
        String fullName,
        String email,
        String phone,
        boolean enabled,
        UUID defaultShippingAddressId,
        UUID defaultBillingAddressId
) {
}
