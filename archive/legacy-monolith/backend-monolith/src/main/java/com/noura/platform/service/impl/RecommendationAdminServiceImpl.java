package com.noura.platform.service.impl;

import com.noura.platform.domain.entity.RecommendationSettings;
import com.noura.platform.dto.recommendation.ProductRecommendationResponse;
import com.noura.platform.dto.recommendation.RecommendationAdminPreviewDto;
import com.noura.platform.dto.recommendation.RecommendationSettingsDto;
import com.noura.platform.dto.recommendation.RecommendationSettingsUpdateRequest;
import com.noura.platform.repository.RecommendationSettingsRepository;
import com.noura.platform.service.RecommendationAdminService;
import com.noura.platform.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecommendationAdminServiceImpl implements RecommendationAdminService {

    private final RecommendationSettingsRepository recommendationSettingsRepository;
    private final RecommendationService recommendationService;

    @Override
    @Transactional(readOnly = true)
    public RecommendationSettingsDto getSettings() {
        return toDto(currentSettings());
    }

    @Override
    @Transactional
    public RecommendationSettingsDto updateSettings(RecommendationSettingsUpdateRequest request, String actor) {
        RecommendationSettings settings = currentSettingsEntity();
        settings.setProductViewWeight(request.productViewWeight());
        settings.setAddToCartWeight(request.addToCartWeight());
        settings.setCheckoutWeight(request.checkoutWeight());
        settings.setTrendingBoost(request.trendingBoost());
        settings.setBestSellerBoost(request.bestSellerBoost());
        settings.setRatingWeight(request.ratingWeight());
        settings.setCategoryAffinityWeight(request.categoryAffinityWeight());
        settings.setBrandAffinityWeight(request.brandAffinityWeight());
        settings.setCoPurchaseWeight(request.coPurchaseWeight());
        settings.setDealBoost(request.dealBoost());
        settings.setMaxRecommendations(request.maxRecommendations());
        if (actor != null && !actor.isBlank() && (settings.getCreatedBy() == null || settings.getCreatedBy().isBlank())) {
            settings.setCreatedBy(actor.trim());
        }
        return toDto(recommendationSettingsRepository.save(settings));
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationAdminPreviewDto preview(String customerRef, UUID productId, int limit) {
        RecommendationSettingsDto settings = getSettings();
        int effectiveLimit = Math.max(1, Math.min(limit, settings.maxRecommendations()));

        ProductRecommendationResponse productPreview = productId == null
                ? null
                : recommendationService.getProductRecommendations(productId, effectiveLimit);

        // Preview uses the same service contracts as storefront traffic so ranking changes remain audit-friendly.
        return new RecommendationAdminPreviewDto(
                settings,
                customerRef,
                productId,
                recommendationService.getTrendingRecommendations(effectiveLimit),
                recommendationService.getBestSellerRecommendations(effectiveLimit),
                recommendationService.getDealRecommendations(effectiveLimit),
                recommendationService.getPersonalizedRecommendations(customerRef, effectiveLimit),
                recommendationService.getCrossSellRecommendations(customerRef, effectiveLimit),
                productPreview
        );
    }

    private RecommendationSettings currentSettings() {
        return recommendationSettingsRepository.findAll().stream().findFirst().orElseGet(RecommendationSettings::new);
    }

    private RecommendationSettings currentSettingsEntity() {
        return recommendationSettingsRepository.findAll().stream().findFirst().orElseGet(RecommendationSettings::new);
    }

    private RecommendationSettingsDto toDto(RecommendationSettings settings) {
        return new RecommendationSettingsDto(
                settings.getId(),
                settings.getProductViewWeight(),
                settings.getAddToCartWeight(),
                settings.getCheckoutWeight(),
                settings.getTrendingBoost(),
                settings.getBestSellerBoost(),
                settings.getRatingWeight(),
                settings.getCategoryAffinityWeight(),
                settings.getBrandAffinityWeight(),
                settings.getCoPurchaseWeight(),
                settings.getDealBoost(),
                settings.getMaxRecommendations()
        );
    }
}
