package com.noura.platform.dto.contract;

import com.noura.platform.domain.enums.MerchantStatus;

import java.time.Instant;
import java.util.UUID;

public record MerchantDto(
        UUID id,
        String name,
        String legalName,
        String taxId,
        String primaryEmail,
        String primaryPhone,
        MerchantStatus status,
        String notes,
        Instant createdAt
) {
}

