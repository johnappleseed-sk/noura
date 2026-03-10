package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.merchandising.MerchandisingBoostDto;
import com.noura.platform.dto.merchandising.MerchandisingBoostRequest;
import com.noura.platform.dto.merchandising.MerchandisingPreviewDto;
import com.noura.platform.dto.merchandising.MerchandisingSettingsDto;
import com.noura.platform.dto.merchandising.MerchandisingSettingsUpdateRequest;
import com.noura.platform.service.MerchandisingAdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MerchandisingAdminController {

    private final MerchandisingAdminService merchandisingAdminService;

    @GetMapping("${app.api.version-prefix:/api/v1}/admin/merchandising/settings")
    public ApiResponse<MerchandisingSettingsDto> getSettings(HttpServletRequest http) {
        return ApiResponse.ok("Merchandising settings", merchandisingAdminService.getSettings(), http.getRequestURI());
    }

    @PutMapping("${app.api.version-prefix:/api/v1}/admin/merchandising/settings")
    public ApiResponse<MerchandisingSettingsDto> updateSettings(
            @Valid @RequestBody MerchandisingSettingsUpdateRequest request,
            Authentication authentication,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Merchandising settings updated",
                merchandisingAdminService.updateSettings(request, authentication == null ? null : authentication.getName()),
                http.getRequestURI()
        );
    }

    @GetMapping("${app.api.version-prefix:/api/v1}/admin/merchandising/boosts")
    public ApiResponse<List<MerchandisingBoostDto>> listBoosts(HttpServletRequest http) {
        return ApiResponse.ok("Merchandising boosts", merchandisingAdminService.listBoosts(), http.getRequestURI());
    }

    @PostMapping("${app.api.version-prefix:/api/v1}/admin/merchandising/boosts")
    public ApiResponse<MerchandisingBoostDto> createBoost(
            @Valid @RequestBody MerchandisingBoostRequest request,
            Authentication authentication,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Merchandising boost created",
                merchandisingAdminService.createBoost(request, authentication == null ? null : authentication.getName()),
                http.getRequestURI()
        );
    }

    @PutMapping("${app.api.version-prefix:/api/v1}/admin/merchandising/boosts/{boostId}")
    public ApiResponse<MerchandisingBoostDto> updateBoost(
            @PathVariable UUID boostId,
            @Valid @RequestBody MerchandisingBoostRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Merchandising boost updated",
                merchandisingAdminService.updateBoost(boostId, request),
                http.getRequestURI()
        );
    }

    @DeleteMapping("${app.api.version-prefix:/api/v1}/admin/merchandising/boosts/{boostId}")
    public ApiResponse<Void> deleteBoost(@PathVariable UUID boostId, HttpServletRequest http) {
        merchandisingAdminService.deleteBoost(boostId);
        return ApiResponse.ok("Merchandising boost deleted", null, http.getRequestURI());
    }

    @GetMapping("${app.api.version-prefix:/api/v1}/admin/merchandising/preview")
    public ApiResponse<MerchandisingPreviewDto> preview(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(defaultValue = "6") int limit,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Merchandising preview",
                merchandisingAdminService.preview(query, categoryId, storeId, limit),
                http.getRequestURI()
        );
    }
}
