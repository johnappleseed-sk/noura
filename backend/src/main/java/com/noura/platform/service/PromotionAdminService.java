package com.noura.platform.service;

import com.noura.platform.dto.pricing.PromotionDto;
import com.noura.platform.dto.pricing.PromotionEvaluationDto;
import com.noura.platform.dto.pricing.PromotionEvaluationRequest;
import com.noura.platform.dto.pricing.PromotionUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface PromotionAdminService {
    List<PromotionDto> listPromotions(String query, Boolean active, Boolean archived);

    PromotionDto getPromotion(UUID promotionId);

    PromotionDto updatePromotion(UUID promotionId, PromotionUpdateRequest request);

    PromotionEvaluationDto evaluatePromotions(PromotionEvaluationRequest request);
}
