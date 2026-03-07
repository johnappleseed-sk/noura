package com.noura.platform.dto.catalog;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CategoryUpdateRequest(
        @NotBlank String name,
        String description,
        String classificationCode,
        UUID parentId,
        UUID managerId
) {
}
