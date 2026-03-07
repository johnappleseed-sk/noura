package com.noura.platform.dto.catalog;

import java.util.UUID;

public record CategoryDto(
        UUID id,
        String name,
        String description,
        String classificationCode,
        UUID parentId,
        UUID managerId,
        int taxonomyVersion
) {
}
