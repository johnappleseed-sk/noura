package com.noura.review.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Storefront review submission request.
 *
 * @param rating star rating from 1 to 5
 * @param title optional review title
 * @param comment free-form review body
 */
public record CreateReviewRequest(
        @Min(value = 1, message = "rating must be at least 1")
        @Max(value = 5, message = "rating must be at most 5")
        int rating,
        @Size(max = 180, message = "title must be 180 characters or fewer")
        String title,
        @NotBlank(message = "comment is required")
        @Size(max = 2000, message = "comment must be 2000 characters or fewer")
        String comment
) {
}
