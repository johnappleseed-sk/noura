package com.noura.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

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

    /**
     * Frontend compatibility alias so search hits can be adapted with less client remapping.
     *
     * @return product id as {@code id}
     */
    @JsonProperty("id")
    public UUID storefrontId() {
        return productId;
    }

    /**
     * Frontend compatibility alias mirroring product-card conventions.
     *
     * @return projected trending flag as {@code isTrending}
     */
    @JsonProperty("isTrending")
    public boolean storefrontTrending() {
        return trending;
    }

    /**
     * Frontend compatibility alias mirroring merchandising-card score naming.
     *
     * @return projected popularity score as {@code merchandisingScore}
     */
    @JsonProperty("merchandisingScore")
    public int storefrontMerchandisingScore() {
        return popularityScore;
    }
}
