package com.noura.platform.dto.catalog;

import java.util.UUID;

public record CategoryTranslationDto(
        UUID id,
        UUID categoryId,
        String locale,
        String localizedName,
        String localizedDescription,
        String seoSlug
) {
}
