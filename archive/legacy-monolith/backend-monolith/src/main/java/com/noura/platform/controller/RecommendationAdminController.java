package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.recommendation.RecommendationAdminPreviewDto;
import com.noura.platform.dto.recommendation.RecommendationSettingsDto;
import com.noura.platform.dto.recommendation.RecommendationSettingsUpdateRequest;
import com.noura.platform.service.RecommendationAdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RecommendationAdminController {

    private final RecommendationAdminService recommendationAdminService;

    @GetMapping("${app.api.version-prefix:/api/v1}/admin/recommendations/settings")
    public ApiResponse<RecommendationSettingsDto> getSettings(HttpServletRequest http) {
        return ApiResponse.ok(
                "Recommendation settings",
                recommendationAdminService.getSettings(),
                http.getRequestURI()
        );
    }

    @PutMapping("${app.api.version-prefix:/api/v1}/admin/recommendations/settings")
    public ApiResponse<RecommendationSettingsDto> updateSettings(
            @Valid @RequestBody RecommendationSettingsUpdateRequest request,
            Authentication authentication,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Recommendation settings updated",
                recommendationAdminService.updateSettings(request, authentication == null ? null : authentication.getName()),
                http.getRequestURI()
        );
    }

    @GetMapping("${app.api.version-prefix:/api/v1}/admin/recommendations/preview")
    public ApiResponse<RecommendationAdminPreviewDto> preview(
            @RequestParam(required = false) String customerRef,
            @RequestParam(required = false) UUID productId,
            @RequestParam(defaultValue = "6") int limit,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Recommendation preview",
                recommendationAdminService.preview(customerRef, productId, limit),
                http.getRequestURI()
        );
    }
}
