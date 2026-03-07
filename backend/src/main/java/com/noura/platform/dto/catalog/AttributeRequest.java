package com.noura.platform.dto.catalog;

import com.noura.platform.domain.enums.AttributeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AttributeRequest(
        @NotBlank String name,
        @NotNull AttributeType type,
        List<String> possibleValues
) {
}
