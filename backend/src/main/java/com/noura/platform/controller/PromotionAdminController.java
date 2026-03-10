package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.pricing.PromotionDto;
import com.noura.platform.dto.pricing.PromotionEvaluationDto;
import com.noura.platform.dto.pricing.PromotionEvaluationRequest;
import com.noura.platform.dto.pricing.PromotionUpdateRequest;
import com.noura.platform.service.PromotionAdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PromotionAdminController {

    private final PromotionAdminService promotionAdminService;

    @GetMapping("${app.api.version-prefix:/api/v1}/admin/promotions")
    public ApiResponse<List<PromotionDto>> listPromotions(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean archived,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Promotions", promotionAdminService.listPromotions(query, active, archived), http.getRequestURI());
    }

    @GetMapping("${app.api.version-prefix:/api/v1}/admin/promotions/{promotionId}")
    public ApiResponse<PromotionDto> getPromotion(@PathVariable UUID promotionId, HttpServletRequest http) {
        return ApiResponse.ok("Promotion", promotionAdminService.getPromotion(promotionId), http.getRequestURI());
    }

    @PatchMapping("${app.api.version-prefix:/api/v1}/admin/promotions/{promotionId}")
    public ApiResponse<PromotionDto> updatePromotion(
            @PathVariable UUID promotionId,
            @Valid @RequestBody PromotionUpdateRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Promotion updated", promotionAdminService.updatePromotion(promotionId, request), http.getRequestURI());
    }

    @PostMapping("${app.api.version-prefix:/api/v1}/admin/promotions/evaluate")
    public ApiResponse<PromotionEvaluationDto> evaluatePromotions(
            @Valid @RequestBody PromotionEvaluationRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Promotion evaluation", promotionAdminService.evaluatePromotions(request), http.getRequestURI());
    }
}
