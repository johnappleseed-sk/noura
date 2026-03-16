package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.domain.enums.StoreServiceType;
import com.noura.platform.dto.location.ForwardGeocodeRequest;
import com.noura.platform.dto.location.GeocodeResultDto;
import com.noura.platform.dto.location.LocationResolveDto;
import com.noura.platform.dto.location.LocationResolveRequest;
import com.noura.platform.dto.location.NearbyStoreDto;
import com.noura.platform.dto.location.ReverseGeocodeRequest;
import com.noura.platform.dto.location.ServiceAreaValidationRequest;
import com.noura.platform.dto.location.ServiceEligibilityDto;
import com.noura.platform.service.LocationGeocodingService;
import com.noura.platform.service.LocationIntelligenceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/location")
public class LocationController {

    private final LocationGeocodingService geocodingService;
    private final LocationIntelligenceService locationIntelligenceService;

    @PostMapping("/reverse-geocode")
    public ApiResponse<GeocodeResultDto> reverseGeocode(@Valid @RequestBody ReverseGeocodeRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Reverse geocode", geocodingService.reverseGeocode(request), http.getRequestURI());
    }

    @PostMapping("/forward-geocode")
    public ApiResponse<List<GeocodeResultDto>> forwardGeocode(@Valid @RequestBody ForwardGeocodeRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Forward geocode", geocodingService.forwardGeocode(request), http.getRequestURI());
    }

    @PostMapping("/validate-service-area")
    public ApiResponse<ServiceEligibilityDto> validateServiceArea(
            @Valid @RequestBody ServiceAreaValidationRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Service eligibility", locationIntelligenceService.validate(request), http.getRequestURI());
    }

    @GetMapping("/nearby-stores")
    public ApiResponse<List<NearbyStoreDto>> nearbyStores(
            @RequestParam("lat") @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
            @RequestParam("lng") @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
            @RequestParam(required = false) StoreServiceType serviceType,
            @RequestParam(required = false) Boolean openNow,
            @RequestParam(defaultValue = "10") @Min(1) @Max(25) int limit,
            @RequestParam(required = false) Integer maxDistanceMeters,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Nearby stores",
                locationIntelligenceService.nearbyStores(latitude, longitude, serviceType, openNow, limit, maxDistanceMeters),
                http.getRequestURI()
        );
    }

    @PostMapping("/resolve")
    public ResponseEntity<ApiResponse<LocationResolveDto>> resolve(
            @Valid @RequestBody LocationResolveRequest request,
            Authentication authentication,
            HttpServletRequest http
    ) {
        String actor = authentication == null ? null : authentication.getName();
        LocationResolveDto resolved = locationIntelligenceService.resolve(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Location resolved", resolved, http.getRequestURI()));
    }
}

