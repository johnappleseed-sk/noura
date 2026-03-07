package com.noura.platform.dto.catalog;

import java.util.List;
import java.util.UUID;

public record CategoryTreeDto(
        UUID id,
        String name,
        String description,
        String classificationCode,
        UUID managerId,
        List<CategoryTreeDto> children
) {
}
