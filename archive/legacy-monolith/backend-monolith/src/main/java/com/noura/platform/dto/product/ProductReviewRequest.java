package com.noura.platform.dto.product;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ProductReviewRequest(
        @Min(1) @Max(5) int rating,
        @NotBlank String comment
) {
}
