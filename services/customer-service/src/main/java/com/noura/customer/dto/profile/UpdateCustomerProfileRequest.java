package com.noura.customer.dto.profile;

import jakarta.validation.constraints.NotBlank;

/**
 * Profile update command payload.
 *
 * @param fullName customer full name
 * @param phone optional phone
 * @param email optional email
 */
public record UpdateCustomerProfileRequest(
        @NotBlank String fullName,
        String phone,
        String email
) {
}
