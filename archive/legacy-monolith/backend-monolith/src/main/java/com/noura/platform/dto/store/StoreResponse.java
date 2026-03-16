package com.noura.platform.dto.store;

import com.noura.platform.domain.enums.StoreStatus;
import com.noura.platform.domain.enums.StoreType;

import java.time.Instant;
import java.util.UUID;

public record StoreResponse(
        UUID id,
        String storeCode,
        UUID merchantId,
        String name,
        String slug,
        StoreType type,
        StoreStatus status,
        String contactEmail,
        String contactPhone,
        String countryCode,
        String city,
        String addressLine1,
        String addressLine2,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy
) {
}
