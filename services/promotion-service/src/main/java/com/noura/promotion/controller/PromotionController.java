package com.noura.promotion.controller;

import com.noura.promotion.common.ApiResponse;
import com.noura.promotion.controller.support.PromotionRequestContextResolver;
import com.noura.promotion.dto.promotion.PromotionEvaluationRequest;
import com.noura.promotion.dto.promotion.PromotionEvaluationResponse;
import com.noura.promotion.dto.promotion.PromotionResponse;
import com.noura.promotion.dto.promotion.PromotionUpsertRequest;
import com.noura.promotion.dto.promotion.PromotionValidationRequest;
import com.noura.promotion.dto.promotion.PromotionValidationResponse;
import com.noura.promotion.exception.PromotionOperationException;
import com.noura.promotion.service.PromotionService;
import com.noura.promotion.service.model.PromotionRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for promotion discovery, promo-code validation, cart evaluation, and admin CRUD.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping
public class PromotionController {

    private final PromotionService promotionService;
    private final PromotionRequestContextResolver contextResolver;

    /**
     * Lists active storefront-visible promotions.
     *
     * @param request current request
     * @return active promotion list
     */
    @GetMapping({"/api/v1/promotions/active", "/api/promotions/active"})
    public ApiResponse<List<PromotionResponse>> listActivePromotions(HttpServletRequest request) {
        List<PromotionResponse> data = promotionService.listActivePromotions();
        return ApiResponse.ok("Active promotions", data, request.getRequestURI());
    }

    /**
     * Validates one promo code against the supplied cart snapshot.
     *
     * @param requestBody validation request
     * @param request current request
     * @return validation result
     */
    @PostMapping({"/api/v1/promotions/validate-code", "/api/promotions/validate-code"})
    public ApiResponse<PromotionValidationResponse> validatePromoCode(
            @Valid @RequestBody PromotionValidationRequest requestBody,
            HttpServletRequest request
    ) {
        PromotionValidationResponse data = promotionService.validatePromoCode(requestBody);
        return ApiResponse.ok("Promotion validation", data, request.getRequestURI());
    }

    /**
     * Evaluates automatic and code-based discounts for a cart snapshot.
     *
     * @param requestBody evaluation request
     * @param request current request
     * @return evaluation result
     */
    @PostMapping({
            "/api/v1/promotions/evaluate",
            "/api/promotions/evaluate",
            "/api/v1/promotions/evaluate-cart",
            "/api/promotions/evaluate-cart"
    })
    public ApiResponse<PromotionEvaluationResponse> evaluateCartDiscount(
            @Valid @RequestBody PromotionEvaluationRequest requestBody,
            HttpServletRequest request
    ) {
        PromotionEvaluationResponse data = promotionService.evaluateCartDiscount(requestBody);
        return ApiResponse.ok("Promotion evaluation", data, request.getRequestURI());
    }

    /**
     * Lists admin-visible promotions with optional filters.
     *
     * @param query optional name/code query
     * @param active optional active-state filter
     * @param archived optional archived-state filter
     * @param request current request
     * @return promotion list
     */
    @GetMapping({"/api/v1/admin/promotions", "/api/admin/promotions"})
    public ApiResponse<List<PromotionResponse>> listPromotions(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean archived,
            HttpServletRequest request
    ) {
        PromotionRequestContext context = contextResolver.resolve(request);
        List<PromotionResponse> data = promotionService.listPromotions(context, query, active, archived);
        return ApiResponse.ok("Promotions", data, request.getRequestURI());
    }

    /**
     * Retrieves one promotion for admin detail views.
     *
     * @param promotionId promotion identifier
     * @param request current request
     * @return promotion
     */
    @GetMapping({"/api/v1/admin/promotions/{promotionId}", "/api/admin/promotions/{promotionId}"})
    public ApiResponse<PromotionResponse> getPromotion(
            @PathVariable UUID promotionId,
            HttpServletRequest request
    ) {
        PromotionRequestContext context = contextResolver.resolve(request);
        PromotionResponse data = promotionService.getPromotion(context, promotionId);
        return ApiResponse.ok("Promotion", data, request.getRequestURI());
    }

    /**
     * Creates one promotion using the legacy storefront/admin-compatible path and the admin path.
     *
     * @param requestBody promotion create payload
     * @param request current request
     * @return created promotion
     */
    @PostMapping({
            "/api/v1/promotions",
            "/api/promotions",
            "/api/v1/admin/promotions",
            "/api/admin/promotions"
    })
    public ResponseEntity<ApiResponse<PromotionResponse>> createPromotion(
            @Valid @RequestBody PromotionUpsertRequest requestBody,
            HttpServletRequest request
    ) {
        PromotionRequestContext context = contextResolver.resolve(request);
        PromotionResponse data = promotionService.createPromotion(context, requestBody);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Promotion created", data, request.getRequestURI()));
    }

    /**
     * Updates one promotion.
     *
     * @param promotionId promotion identifier
     * @param requestBody promotion update payload
     * @param request current request
     * @return updated promotion
     */
    @PatchMapping({"/api/v1/admin/promotions/{promotionId}", "/api/admin/promotions/{promotionId}"})
    public ApiResponse<PromotionResponse> updatePromotion(
            @PathVariable UUID promotionId,
            @Valid @RequestBody PromotionUpsertRequest requestBody,
            HttpServletRequest request
    ) {
        PromotionRequestContext context = contextResolver.resolve(request);
        PromotionResponse data = promotionService.updatePromotion(context, promotionId, requestBody);
        return ApiResponse.ok("Promotion updated", data, request.getRequestURI());
    }

    /**
     * Deletes one promotion permanently.
     *
     * @param promotionId promotion identifier
     * @param request current request
     * @return delete result
     */
    @DeleteMapping({"/api/v1/admin/promotions/{promotionId}", "/api/admin/promotions/{promotionId}"})
    public ApiResponse<Void> deletePromotion(
            @PathVariable UUID promotionId,
            HttpServletRequest request
    ) {
        PromotionRequestContext context = contextResolver.resolve(request);
        promotionService.deletePromotion(context, promotionId);
        return ApiResponse.ok("Promotion deleted", null, request.getRequestURI());
    }

    /**
     * Evaluates promotions through the admin endpoint shape already used by the operations console.
     *
     * @param requestBody evaluation request
     * @param request current request
     * @return evaluation result
     */
    @PostMapping({"/api/v1/admin/promotions/evaluate", "/api/admin/promotions/evaluate"})
    public ApiResponse<PromotionEvaluationResponse> evaluatePromotionsForAdmin(
            @Valid @RequestBody PromotionEvaluationRequest requestBody,
            HttpServletRequest request
    ) {
        PromotionRequestContext context = contextResolver.resolve(request);
        if (!context.canManageAllPromotions()) {
            throw new PromotionOperationException(
                    HttpStatus.FORBIDDEN,
                    "PROMOTION_FORBIDDEN",
                    "Promotion management requires admin or marketing permissions"
            );
        }
        PromotionEvaluationResponse data = promotionService.evaluateCartDiscount(requestBody);
        return ApiResponse.ok("Promotion evaluation", data, request.getRequestURI());
    }
}
