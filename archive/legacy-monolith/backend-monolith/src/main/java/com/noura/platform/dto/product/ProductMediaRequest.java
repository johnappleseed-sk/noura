package com.noura.platform.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductMediaRequest(
        @NotBlank String mediaType,
        @NotBlank String url,
        @PositiveOrZero int sortOrder,
        boolean isPrimary
) {
}
