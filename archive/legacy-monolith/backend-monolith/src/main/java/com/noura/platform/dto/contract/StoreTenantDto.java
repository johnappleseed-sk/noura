package com.noura.platform.dto.contract;

import com.noura.platform.domain.enums.StoreTenantStatus;

import java.time.Instant;
import java.util.UUID;

public record StoreTenantDto(
        UUID id,
        UUID storeId,
        String storeName,
        UUID merchantId,
        String merchantName,
        UUID contractId,
        String contractNumber,
        StoreTenantStatus status,
        Instant activatedAt,
        Instant deactivatedAt,
        Instant createdAt
) {
}

