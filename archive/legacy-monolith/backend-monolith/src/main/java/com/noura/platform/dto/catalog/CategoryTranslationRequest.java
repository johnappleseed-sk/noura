package com.noura.platform.dto.catalog;

import jakarta.validation.constraints.NotBlank;

public record CategoryTranslationRequest(
        @NotBlank String localizedName,
        String localizedDescription,
        String seoSlug
) {
}
