package com.noura.platform.dto.category;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        UUID parentId,
        String code,
        String name,
        String slug,
        Integer level,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
