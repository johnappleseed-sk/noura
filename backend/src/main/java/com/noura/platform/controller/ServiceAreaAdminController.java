package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.common.api.PaginationUtils;
import com.noura.platform.domain.enums.ServiceAreaStatus;
import com.noura.platform.domain.enums.ServiceAreaType;
import com.noura.platform.dto.location.ServiceAreaDto;
import com.noura.platform.dto.location.ServiceAreaRequest;
import com.noura.platform.dto.location.ServiceAreaValidationRequest;
import com.noura.platform.dto.location.ServiceEligibilityDto;
import com.noura.platform.service.LocationIntelligenceService;
import com.noura.platform.service.ServiceAreaAdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ServiceAreaAdminController {

    private final ServiceAreaAdminService serviceAreaAdminService;
    private final LocationIntelligenceService locationIntelligenceService;

    @GetMapping("${app.api.version-prefix:/api/v1}/admin/service-areas")
    public ApiResponse<PageResponse<ServiceAreaDto>> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) ServiceAreaStatus status,
            @RequestParam(required = false) ServiceAreaType type,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = PaginationUtils.pageOf(page, size, sortBy, direction);
        Page<ServiceAreaDto> result = serviceAreaAdminService.list(query, status, type, pageable);
        return ApiResponse.ok("Service areas", PageResponse.from(result), http.getRequestURI());
    }

    @GetMapping("${app.api.version-prefix:/api/v1}/admin/service-areas/{serviceAreaId}")
    public ApiResponse<ServiceAreaDto> get(@PathVariable UUID serviceAreaId, HttpServletRequest http) {
        return ApiResponse.ok("Service area", serviceAreaAdminService.get(serviceAreaId), http.getRequestURI());
    }

    @PostMapping("${app.api.version-prefix:/api/v1}/admin/service-areas")
    public ResponseEntity<ApiResponse<ServiceAreaDto>> create(
            @Valid @RequestBody ServiceAreaRequest request,
            Authentication authentication,
            HttpServletRequest http
    ) {
        String actor = authentication == null ? null : authentication.getName();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Service area created", serviceAreaAdminService.create(request, actor), http.getRequestURI()));
    }

    @PutMapping("${app.api.version-prefix:/api/v1}/admin/service-areas/{serviceAreaId}")
    public ApiResponse<ServiceAreaDto> update(
            @PathVariable UUID serviceAreaId,
            @Valid @RequestBody ServiceAreaRequest request,
            Authentication authentication,
            HttpServletRequest http
    ) {
        String actor = authentication == null ? null : authentication.getName();
        return ApiResponse.ok("Service area updated", serviceAreaAdminService.update(serviceAreaId, request, actor), http.getRequestURI());
    }

    @DeleteMapping("${app.api.version-prefix:/api/v1}/admin/service-areas/{serviceAreaId}")
    public ApiResponse<Void> delete(
            @PathVariable UUID serviceAreaId,
            Authentication authentication,
            HttpServletRequest http
    ) {
        String actor = authentication == null ? null : authentication.getName();
        serviceAreaAdminService.delete(serviceAreaId, actor);
        return ApiResponse.ok("Service area deleted", null, http.getRequestURI());
    }

    @PostMapping("${app.api.version-prefix:/api/v1}/admin/service-areas/{serviceAreaId}/activate")
    public ApiResponse<ServiceAreaDto> activate(
            @PathVariable UUID serviceAreaId,
            Authentication authentication,
            HttpServletRequest http
    ) {
        String actor = authentication == null ? null : authentication.getName();
        return ApiResponse.ok("Service area activated", serviceAreaAdminService.activate(serviceAreaId, actor), http.getRequestURI());
    }

    @PostMapping("${app.api.version-prefix:/api/v1}/admin/service-areas/{serviceAreaId}/deactivate")
    public ApiResponse<ServiceAreaDto> deactivate(
            @PathVariable UUID serviceAreaId,
            Authentication authentication,
            HttpServletRequest http
    ) {
        String actor = authentication == null ? null : authentication.getName();
        return ApiResponse.ok("Service area deactivated", serviceAreaAdminService.deactivate(serviceAreaId, actor), http.getRequestURI());
    }

    @PostMapping("${app.api.version-prefix:/api/v1}/admin/service-areas/validate")
    public ApiResponse<ServiceEligibilityDto> validate(
            @Valid @RequestBody ServiceAreaValidationRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Service eligibility", locationIntelligenceService.validate(request), http.getRequestURI());
    }
}

