package com.noura.platform.dto.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;
import java.util.UUID;

public record AttributeSetRequest(
        @NotBlank String name,
        @NotEmpty Set<UUID> attributeIds
) {
}
