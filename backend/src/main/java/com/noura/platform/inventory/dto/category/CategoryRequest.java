package com.noura.platform.inventory.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @Size(max = 36) String parentId,
        @NotBlank @Size(max = 100) String categoryCode,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 5000) String description,
        Integer sortOrder,
        Boolean active
) {
}
