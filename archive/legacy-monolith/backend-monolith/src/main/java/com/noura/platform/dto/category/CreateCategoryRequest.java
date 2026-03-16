package com.noura.platform.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCategoryRequest(
        UUID parentId,
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String slug,
        Boolean active
) {
}
