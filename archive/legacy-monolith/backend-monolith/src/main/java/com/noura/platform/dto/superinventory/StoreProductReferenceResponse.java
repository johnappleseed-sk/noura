package com.noura.platform.dto.superinventory;

import java.time.Instant;
import java.util.UUID;

public record StoreProductReferenceResponse(
        UUID id,
        UUID storeId,
        UUID productId,
        boolean active,
        Instant createdAt
) {
}
