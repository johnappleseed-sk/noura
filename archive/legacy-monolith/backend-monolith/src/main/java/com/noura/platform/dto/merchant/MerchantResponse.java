package com.noura.platform.dto.merchant;

import com.noura.platform.domain.enums.MerchantStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MerchantResponse(
        UUID id,
        String merchantCode,
        String legalName,
        String displayName,
        String email,
        String phone,
        String countryCode,
        MerchantStatus status,
        LocalDate contractStartAt,
        LocalDate contractEndAt,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy
) {
}
