package com.noura.catalog.controller;

import com.noura.catalog.common.ApiResponse;
import com.noura.catalog.dto.admin.MerchandisingBoostRequest;
import com.noura.catalog.dto.admin.MerchandisingBoostResponse;
import com.noura.catalog.dto.admin.MerchandisingPreviewResponse;
import com.noura.catalog.dto.admin.MerchandisingSettingsResponse;
import com.noura.catalog.dto.admin.MerchandisingSettingsUpdateRequest;
import com.noura.catalog.dto.admin.RecommendationAdminPreviewResponse;
import com.noura.catalog.dto.admin.RecommendationSettingsResponse;
import com.noura.catalog.dto.admin.RecommendationSettingsUpdateRequest;
import com.noura.catalog.service.CatalogAdminCompatibilityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Admin-web compatibility controller for recommendation and merchandising control pages.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/admin")
public class CatalogAdminCompatibilityController {

    private static final String SUBJECT_HEADER = "X-Auth-Subject";

    private final CatalogAdminCompatibilityService compatibilityService;

    @GetMapping("/recommendations/settings")
    public ApiResponse<RecommendationSettingsResponse> getRecommendationSettings(HttpServletRequest request) {
        return ApiResponse.ok(
                "Recommendation settings",
                compatibilityService.getRecommendationSettings(),
                request.getRequestURI()
        );
    }

    @PutMapping("/recommendations/settings")
    public ApiResponse<RecommendationSettingsResponse> updateRecommendationSettings(
            @Valid @RequestBody RecommendationSettingsUpdateRequest payload,
            @RequestHeader(value = SUBJECT_HEADER, required = false) String actorUserId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Recommendation settings updated",
                compatibilityService.updateRecommendationSettings(payload, actorUserId),
                request.getRequestURI()
        );
    }

    @GetMapping("/recommendations/preview")
    public ApiResponse<RecommendationAdminPreviewResponse> previewRecommendations(
            @RequestParam(required = false) String customerRef,
            @RequestParam(required = false) UUID productId,
            @RequestParam(defaultValue = "6") int limit,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Recommendation preview",
                compatibilityService.previewRecommendations(customerRef, productId, limit),
                request.getRequestURI()
        );
    }

    @GetMapping("/merchandising/settings")
    public ApiResponse<MerchandisingSettingsResponse> getMerchandisingSettings(HttpServletRequest request) {
        return ApiResponse.ok(
                "Merchandising settings",
                compatibilityService.getMerchandisingSettings(),
                request.getRequestURI()
        );
    }

    @PutMapping("/merchandising/settings")
    public ApiResponse<MerchandisingSettingsResponse> updateMerchandisingSettings(
            @Valid @RequestBody MerchandisingSettingsUpdateRequest payload,
            @RequestHeader(value = SUBJECT_HEADER, required = false) String actorUserId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Merchandising settings updated",
                compatibilityService.updateMerchandisingSettings(payload, actorUserId),
                request.getRequestURI()
        );
    }

    @GetMapping("/merchandising/boosts")
    public ApiResponse<List<MerchandisingBoostResponse>> listMerchandisingBoosts(HttpServletRequest request) {
        return ApiResponse.ok(
                "Merchandising boosts",
                compatibilityService.listMerchandisingBoosts(),
                request.getRequestURI()
        );
    }

    @PostMapping("/merchandising/boosts")
    public ResponseEntity<ApiResponse<MerchandisingBoostResponse>> createMerchandisingBoost(
            @Valid @RequestBody MerchandisingBoostRequest payload,
            @RequestHeader(value = SUBJECT_HEADER, required = false) String actorUserId,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        "Merchandising boost created",
                        compatibilityService.createMerchandisingBoost(payload, actorUserId),
                        request.getRequestURI()
                ));
    }

    @PutMapping("/merchandising/boosts/{boostId}")
    public ApiResponse<MerchandisingBoostResponse> updateMerchandisingBoost(
            @PathVariable UUID boostId,
            @Valid @RequestBody MerchandisingBoostRequest payload,
            @RequestHeader(value = SUBJECT_HEADER, required = false) String actorUserId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Merchandising boost updated",
                compatibilityService.updateMerchandisingBoost(boostId, payload, actorUserId),
                request.getRequestURI()
        );
    }

    @DeleteMapping("/merchandising/boosts/{boostId}")
    public ApiResponse<Void> deleteMerchandisingBoost(
            @PathVariable UUID boostId,
            HttpServletRequest request
    ) {
        compatibilityService.deleteMerchandisingBoost(boostId);
        return ApiResponse.ok("Merchandising boost deleted", null, request.getRequestURI());
    }

    @GetMapping("/merchandising/preview")
    public ApiResponse<MerchandisingPreviewResponse> previewMerchandising(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(defaultValue = "6") int limit,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Merchandising preview",
                compatibilityService.previewMerchandising(query, categoryId, storeId, limit),
                request.getRequestURI()
        );
    }
}
