package com.noura.platform.dto.contract;

import java.time.Instant;
import java.util.UUID;

public record StoreStaffAssignmentDto(
        UUID id,
        UUID userId,
        String userEmail,
        UUID storeId,
        String storeName,
        String roleCode,
        boolean active,
        Instant createdAt
) {
}

