package com.noura.platform.inventory.dto.category;

import java.time.Instant;

public record CategoryResponse(
        String id,
        String parentId,
        String categoryCode,
        String name,
        String description,
        int level,
        int sortOrder,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
