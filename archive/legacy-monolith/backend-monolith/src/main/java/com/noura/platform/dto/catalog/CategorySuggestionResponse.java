package com.noura.platform.dto.catalog;

import java.util.List;

public record CategorySuggestionResponse(
        List<CategorySuggestionItemDto> suggestions
) {
}
