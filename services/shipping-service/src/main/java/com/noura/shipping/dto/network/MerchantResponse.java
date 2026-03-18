package com.noura.shipping.dto.network;

import com.noura.shipping.domain.enums.MerchantStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Merchant response aligned with admin-web merchant pages.
 */
public record MerchantResponse(
        UUID id,
        String merchantCode,
        String legalName,
        String displayName,
        String email,
        String phone,
        String countryCode,
        MerchantStatus status,
        Instant contractStartAt,
        Instant contractEndAt,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
