package com.noura.platform.service.impl;

import com.noura.platform.domain.entity.AnalyticsEventRecord;
import com.noura.platform.domain.entity.Product;
import com.noura.platform.domain.entity.RecommendationSettings;
import com.noura.platform.domain.enums.AnalyticsEventType;
import com.noura.platform.dto.recommendation.RecommendationProductDto;
import com.noura.platform.repository.AnalyticsEventRecordRepository;
import com.noura.platform.repository.OrderItemRepository;
import com.noura.platform.repository.ProductRepository;
import com.noura.platform.repository.RecommendationSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private AnalyticsEventRecordRepository analyticsEventRecordRepository;

    @Mock
    private RecommendationSettingsRepository recommendationSettingsRepository;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    @Test
    void trendingRecommendationsPrioritizeVisibleSignalWeightedProducts() {
        Product trending = product("Trending", 10, true, true, true, 4.5);
        Product regular = product("Regular", 20, false, false, true, 3.0);
        Product inactive = product("Hidden", 999, true, true, false, 5.0);

        AnalyticsEventRecord cartSignal = new AnalyticsEventRecord();
        cartSignal.setEventType(AnalyticsEventType.ADD_TO_CART);
        cartSignal.setProductId(trending.getId().toString());
        cartSignal.setOccurredAt(Instant.now());

        when(recommendationSettingsRepository.findAll()).thenReturn(List.of(defaultSettings()));
        when(productRepository.findAll()).thenReturn(List.of(trending, regular, inactive));
        when(productRepository.findAllById(any())).thenReturn(List.of(trending, regular));
        when(analyticsEventRecordRepository.findAll()).thenReturn(List.of(cartSignal));

        List<RecommendationProductDto> recommendations = recommendationService.getTrendingRecommendations(3);

        assertEquals(2, recommendations.size());
        assertEquals(trending.getId(), recommendations.get(0).id());
        assertEquals(regular.getId(), recommendations.get(1).id());
    }

    @Test
    void personalizedRecommendationsFallBackWhenCustomerSignalsAreMissing() {
        Product bestSeller = product("Best Seller", 12, false, true, true, 4.8);

        when(recommendationSettingsRepository.findAll()).thenReturn(List.of(defaultSettings()));
        when(productRepository.findAll()).thenReturn(List.of(bestSeller));
        when(productRepository.findAllById(any())).thenReturn(List.of(bestSeller));
        when(analyticsEventRecordRepository.findAll()).thenReturn(List.of());

        List<RecommendationProductDto> recommendations =
                recommendationService.getPersonalizedRecommendations("customer@example.com", 4);

        assertFalse(recommendations.isEmpty());
        assertEquals(bestSeller.getId(), recommendations.get(0).id());
    }

    private RecommendationSettings defaultSettings() {
        RecommendationSettings settings = new RecommendationSettings();
        settings.setProductViewWeight(1D);
        settings.setAddToCartWeight(4D);
        settings.setCheckoutWeight(8D);
        settings.setTrendingBoost(30D);
        settings.setBestSellerBoost(20D);
        settings.setRatingWeight(5D);
        settings.setCategoryAffinityWeight(6D);
        settings.setBrandAffinityWeight(3D);
        settings.setCoPurchaseWeight(5D);
        settings.setDealBoost(60D);
        settings.setMaxRecommendations(12);
        return settings;
    }

    private Product product(String name, int popularityScore, boolean trending, boolean bestSeller, boolean active, double rating) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName(name);
        product.setBasePrice(BigDecimal.valueOf(12.50));
        product.setAttributes(new LinkedHashMap<>());
        product.setPopularityScore(popularityScore);
        product.setTrending(trending);
        product.setBestSeller(bestSeller);
        product.setActive(active);
        product.setAverageRating(rating);
        return product;
    }
}
