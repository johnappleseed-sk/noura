package com.noura.customer.dto.internal;

import java.util.UUID;

/**
 * Internal customer lookup response DTO.
 *
 * @param id customer profile identifier
 * @param externalSubject external subject key
 * @param fullName full name
 * @param email email address
 * @param phone phone number
 * @param enabled account active status
 * @param defaultShippingAddressId default shipping address identifier
 * @param defaultBillingAddressId default billing address identifier
 */
public record CustomerLookupResponse(
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
