package com.noura.platform.dto.product;

import java.time.Instant;
import java.util.UUID;

public record ProductVariantResponse(
        UUID id,
        UUID productId,
        String sku,
        String variantName,
        String barcode,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
