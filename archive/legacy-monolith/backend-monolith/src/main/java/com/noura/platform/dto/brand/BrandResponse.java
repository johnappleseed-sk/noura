package com.noura.platform.dto.brand;

import java.time.Instant;
import java.util.UUID;

public record BrandResponse(
        UUID id,
        String code,
        String name,
        String slug,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
