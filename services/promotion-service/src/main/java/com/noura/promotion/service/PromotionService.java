package com.noura.promotion.service;

import com.noura.promotion.dto.promotion.PromotionEvaluationRequest;
import com.noura.promotion.dto.promotion.PromotionEvaluationResponse;
import com.noura.promotion.dto.promotion.PromotionResponse;
import com.noura.promotion.dto.promotion.PromotionUpsertRequest;
import com.noura.promotion.dto.promotion.PromotionValidationRequest;
import com.noura.promotion.dto.promotion.PromotionValidationResponse;
import com.noura.promotion.service.model.PromotionRequestContext;

import java.util.List;
import java.util.UUID;

/**
 * Application service for promotion admin management and deterministic discount evaluation.
 */
public interface PromotionService {

    /**
     * Lists active non-archived promotions.
     *
     * @return active promotion list
     */
    List<PromotionResponse> listActivePromotions();

    /**
     * Validates one promo code against current eligibility input.
     *
     * @param request validation request
     * @return validation response
     */
    PromotionValidationResponse validatePromoCode(PromotionValidationRequest request);

    /**
     * Evaluates cart discounts for the supplied cart or checkout snapshot.
     *
     * @param request evaluation request
     * @return evaluation result
     */
    PromotionEvaluationResponse evaluateCartDiscount(PromotionEvaluationRequest request);

    /**
     * Lists admin-visible promotions.
     *
     * @param context request context
     * @param query optional text query
     * @param active optional active filter
     * @param archived optional archived filter
     * @return promotion list
     */
    List<PromotionResponse> listPromotions(PromotionRequestContext context, String query, Boolean active, Boolean archived);

    /**
     * Retrieves one promotion by identifier.
     *
     * @param context request context
     * @param promotionId promotion identifier
     * @return promotion response
     */
    PromotionResponse getPromotion(PromotionRequestContext context, UUID promotionId);

    /**
     * Creates one promotion.
     *
     * @param context request context
     * @param request upsert request
     * @return created promotion
     */
    PromotionResponse createPromotion(PromotionRequestContext context, PromotionUpsertRequest request);

    /**
     * Updates one promotion.
     *
     * @param context request context
     * @param promotionId promotion identifier
     * @param request upsert request
     * @return updated promotion
     */
    PromotionResponse updatePromotion(PromotionRequestContext context, UUID promotionId, PromotionUpsertRequest request);

    /**
     * Deletes one promotion.
     *
     * @param context request context
     * @param promotionId promotion identifier
     */
    void deletePromotion(PromotionRequestContext context, UUID promotionId);
}
