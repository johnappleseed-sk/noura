package com.noura.shipping.dto.network;

import com.noura.shipping.domain.enums.MerchantStatus;
import jakarta.validation.constraints.NotBlank;

/**
 * Merchant creation payload aligned with admin-web.
 */
public record MerchantCreateRequest(
        @NotBlank String merchantCode,
        @NotBlank String legalName,
        @NotBlank String displayName,
        String email,
        String phone,
        String countryCode,
        MerchantStatus status,
        String contractStartAt,
        String contractEndAt,
        String notes
) {
}
