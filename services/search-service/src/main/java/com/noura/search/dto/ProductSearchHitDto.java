package com.noura.search.dto;

import java.util.UUID;

/**
 * Public product-search hit returned by search-service.
 *
 * @param productId product identifier
 * @param productCode optional product code
 * @param name product display name
 * @param slug optional storefront slug
 * @param categoryId category identifier
 * @param categoryName category display name
 * @param brandId brand identifier
 * @param brandName brand display name
 * @param averageRating current projected average rating
 * @param reviewCount projected review count
 * @param trending projected trending flag
 * @param popularityScore projected popularity score
 */
public record ProductSearchHitDto(
        UUID productId,
        String productCode,
        String name,
        String slug,
        UUID categoryId,
        String categoryName,
        UUID brandId,
        String brandName,
        double averageRating,
        int reviewCount,
        boolean trending,
        int popularityScore
) {
}
