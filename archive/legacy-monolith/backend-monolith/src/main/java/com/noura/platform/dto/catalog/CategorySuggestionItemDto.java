package com.noura.platform.dto.catalog;

import java.util.List;
import java.util.UUID;

public record CategorySuggestionItemDto(
        UUID categoryId,
        String categoryName,
        double confidence,
        List<String> matchedSignals
) {
}
