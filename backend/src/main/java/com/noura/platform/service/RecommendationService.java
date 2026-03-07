package com.noura.platform.service;

import com.noura.platform.dto.product.ProductDto;

import java.util.List;

public interface RecommendationService {
    /**
     * Executes personalized.
     *
     * @return A list of matching items.
     */
    List<ProductDto> personalized();

    /**
     * Executes cross sell.
     *
     * @return A list of matching items.
     */
    List<ProductDto> crossSell();

    /**
     * Executes best sellers.
     *
     * @return A list of matching items.
     */
    List<ProductDto> bestSellers();

    /**
     * Executes trending.
     *
     * @return A list of matching items.
     */
    List<ProductDto> trending();

    /**
     * Executes deals.
     *
     * @return A list of matching items.
     */
    List<ProductDto> deals();
}
