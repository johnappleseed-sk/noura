package com.noura.search.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Internal product-document upsert payload for the search projection.
 *
 * @param productId product identifier
 * @param productCode optional product code
 * @param name product name
 * @param slug optional storefront slug
 * @param categoryId optional category identifier
 * @param categoryName optional category display name
 * @param brandId optional brand identifier
 * @param brandName optional brand display name
 * @param shortDescription optional short description
 * @param active current product active flag
 * @param trending projected trending flag
 * @param popularityScore projected popularity score
 * @param averageRating projected average rating
 * @param reviewCount projected review count
 * @param sourceUpdatedAt source-system update timestamp
 */
public record ProductSearchDocumentUpsertRequest(
        @NotNull(message = "productId is required")
        UUID productId,
        @Size(max = 120, message = "productCode must be 120 characters or fewer")
        String productCode,
        @NotBlank(message = "name is required")
        @Size(max = 255, message = "name must be 255 characters or fewer")
        String name,
        @Size(max = 255, message = "slug must be 255 characters or fewer")
        String slug,
        UUID categoryId,
        @Size(max = 255, message = "categoryName must be 255 characters or fewer")
        String categoryName,
        UUID brandId,
        @Size(max = 255, message = "brandName must be 255 characters or fewer")
        String brandName,
        @Size(max = 1000, message = "shortDescription must be 1000 characters or fewer")
        String shortDescription,
        boolean active,
        boolean trending,
        @Min(value = 0, message = "popularityScore must be non-negative")
        int popularityScore,
        @Min(value = 0, message = "averageRating must be at least 0")
        @Max(value = 5, message = "averageRating must be at most 5")
        double averageRating,
        @Min(value = 0, message = "reviewCount must be non-negative")
        int reviewCount,
        Instant sourceUpdatedAt
) {
}
