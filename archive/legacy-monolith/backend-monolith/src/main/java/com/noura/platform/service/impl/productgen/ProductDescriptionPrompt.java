package com.noura.platform.service.impl.productgen;

public record ProductDescriptionPrompt(
        String productName,
        String category,
        String brand,
        String targetAudience
) {
}
