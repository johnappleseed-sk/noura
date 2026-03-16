package com.noura.platform.dto.catalog;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record CategorySuggestionRequest(
        @NotBlank String title,
        String description,
        Map<String, Object> attributes,
        Integer maxSuggestions,
        String locale
) {
}
