package com.noura.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TrendTagDto(
        String value,
        int score
) {

    /**
     * Storefront compatibility alias used by existing product-listing pages.
     *
     * @return tag value as {@code name}
     */
    @JsonProperty("name")
    public String storefrontName() {
        return value;
    }

    /**
     * Legacy compatibility alias for consumers expecting {@code tag}.
     *
     * @return tag value as {@code tag}
     */
    @JsonProperty("tag")
    public String storefrontTag() {
        return value;
    }
}
