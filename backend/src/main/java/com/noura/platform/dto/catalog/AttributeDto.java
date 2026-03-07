package com.noura.platform.dto.catalog;

import com.noura.platform.domain.enums.AttributeType;

import java.util.List;
import java.util.UUID;

public record AttributeDto(
        UUID id,
        String name,
        AttributeType type,
        List<String> possibleValues
) {
}
