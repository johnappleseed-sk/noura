package com.noura.platform.service.impl;

import com.noura.platform.domain.entity.Product;
import com.noura.platform.dto.product.ProductDto;
import com.noura.platform.service.ProductService;
import com.noura.platform.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final com.noura.platform.repository.ProductRepository productRepository;
    private final ProductService productService;

    /**
     * Executes personalized.
     *
     * @return A list of matching items.
     */
    @Override
    @Cacheable(cacheNames = "recommendations", key = "'personalized'")
    public List<ProductDto> personalized() {
        return productRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Product::getPopularityScore).reversed())
                .limit(8)
                .map(Product::getId)
                .map(productService::getProduct)
                .toList();
    }

    /**
     * Executes cross sell.
     *
     * @return A list of matching items.
     */
    @Override
    @Cacheable(cacheNames = "recommendations", key = "'crossSell'")
    public List<ProductDto> crossSell() {
        return productRepository.findAll().stream()
                .filter(Product::isBestSeller)
                .limit(6)
                .map(Product::getId)
                .map(productService::getProduct)
                .toList();
    }

    /**
     * Executes best sellers.
     *
     * @return A list of matching items.
     */
    @Override
    @Cacheable(cacheNames = "recommendations", key = "'bestSellers'")
    public List<ProductDto> bestSellers() {
        return productRepository.findTop10ByBestSellerTrueOrderByPopularityScoreDesc()
                .stream()
                .map(Product::getId)
                .map(productService::getProduct)
                .toList();
    }

    /**
     * Executes trending.
     *
     * @return A list of matching items.
     */
    @Override
    @Cacheable(cacheNames = "recommendations", key = "'trending'")
    public List<ProductDto> trending() {
        return productRepository.findTop10ByTrendingTrueOrderByPopularityScoreDesc()
                .stream()
                .map(Product::getId)
                .map(productService::getProduct)
                .toList();
    }

    /**
     * Executes deals.
     *
     * @return A list of matching items.
     */
    @Override
    @Cacheable(cacheNames = "recommendations", key = "'deals'")
    public List<ProductDto> deals() {
        return productRepository.findAll().stream()
                .filter(product -> product.getBasePrice().doubleValue() <= 500)
                .sorted(Comparator.comparing(Product::getBasePrice))
                .limit(10)
                .map(Product::getId)
                .map(productService::getProduct)
                .toList();
    }
}
