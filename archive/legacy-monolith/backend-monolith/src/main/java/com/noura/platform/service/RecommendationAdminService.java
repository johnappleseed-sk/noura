package com.noura.platform.service;

import com.noura.platform.dto.recommendation.RecommendationAdminPreviewDto;
import com.noura.platform.dto.recommendation.RecommendationSettingsDto;
import com.noura.platform.dto.recommendation.RecommendationSettingsUpdateRequest;

import java.util.UUID;

public interface RecommendationAdminService {
    RecommendationSettingsDto getSettings();

    RecommendationSettingsDto updateSettings(RecommendationSettingsUpdateRequest request, String actor);

    RecommendationAdminPreviewDto preview(String customerRef, UUID productId, int limit);
}
