package com.noura.platform.dto.catalog;

import java.util.List;
import java.util.UUID;

public record AttributeSetDto(
        UUID id,
        String name,
        List<AttributeDto> attributes
) {
}
