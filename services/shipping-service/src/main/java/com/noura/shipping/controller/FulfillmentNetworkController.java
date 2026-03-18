package com.noura.shipping.controller;

import com.noura.shipping.common.ApiResponse;
import com.noura.shipping.common.PageResponse;
import com.noura.shipping.domain.enums.MerchantStatus;
import com.noura.shipping.domain.enums.ServiceAreaStatus;
import com.noura.shipping.domain.enums.ServiceAreaType;
import com.noura.shipping.domain.enums.StoreStatus;
import com.noura.shipping.domain.enums.StoreType;
import com.noura.shipping.dto.network.MerchantCreateRequest;
import com.noura.shipping.dto.network.MerchantResponse;
import com.noura.shipping.dto.network.MerchantStatusUpdateRequest;
import com.noura.shipping.dto.network.ServiceAreaRequest;
import com.noura.shipping.dto.network.ServiceAreaResponse;
import com.noura.shipping.dto.network.ServiceAreaValidationRequest;
import com.noura.shipping.dto.network.ServiceEligibilityResponse;
import com.noura.shipping.dto.network.StoreLocationResponse;
import com.noura.shipping.dto.network.StoreLocationUpdateRequest;
import com.noura.shipping.dto.network.StoreRequest;
import com.noura.shipping.dto.network.StoreResponse;
import com.noura.shipping.dto.network.StoreStatusUpdateRequest;
import com.noura.shipping.service.FulfillmentNetworkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Shipping-owned admin compatibility controller for merchants, stores, and service areas.
 */
@Validated
@RestController
@RequiredArgsConstructor
public class FulfillmentNetworkController {

    private static final String SUBJECT_HEADER = "X-Auth-Subject";

    private final FulfillmentNetworkService fulfillmentNetworkService;

    @GetMapping("/api/v1/admin/merchants")
    public ApiResponse<PageResponse<MerchantResponse>> listMerchants(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) MerchantStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Merchants",
                fulfillmentNetworkService.listMerchants(search, status, page, size, sortBy, direction),
                request.getRequestURI()
        );
    }

    @GetMapping("/api/v1/admin/merchants/{merchantId}")
    public ApiResponse<MerchantResponse> getMerchant(
            @PathVariable UUID merchantId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok("Merchant", fulfillmentNetworkService.getMerchant(merchantId), request.getRequestURI());
    }

    @PostMapping("/api/v1/admin/merchants")
    public ResponseEntity<ApiResponse<MerchantResponse>> createMerchant(
            @Valid @RequestBody MerchantCreateRequest payload,
            @RequestHeader(value = SUBJECT_HEADER, required = false) String actorUserId,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        "Merchant created",
                        fulfillmentNetworkService.createMerchant(payload, actorUserId),
                        request.getRequestURI()
                ));
    }

    @PatchMapping("/api/v1/admin/merchants/{merchantId}/status")
    public ApiResponse<MerchantResponse> updateMerchantStatus(
            @PathVariable UUID merchantId,
            @Valid @RequestBody MerchantStatusUpdateRequest payload,
            @RequestHeader(value = SUBJECT_HEADER, required = false) String actorUserId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Merchant status updated",
                fulfillmentNetworkService.updateMerchantStatus(merchantId, payload, actorUserId),
                request.getRequestURI()
        );
    }

    @GetMapping("/api/v1/admin/stores")
    public ApiResponse<PageResponse<StoreResponse>> listAdminStores(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID merchantId,
            @RequestParam(required = false) StoreType type,
            @RequestParam(required = false) StoreStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Stores",
                fulfillmentNetworkService.listAdminStores(search, merchantId, type, status, page, size, sortBy, direction),
                request.getRequestURI()
        );
    }

    @GetMapping("/api/v1/admin/stores/{storeId}")
    public ApiResponse<StoreResponse> getAdminStore(
            @PathVariable UUID storeId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok("Store", fulfillmentNetworkService.getAdminStore(storeId), request.getRequestURI());
    }

    @PostMapping("/api/v1/admin/stores")
    public ResponseEntity<ApiResponse<StoreResponse>> createAdminStore(
            @Valid @RequestBody StoreRequest payload,
            @RequestHeader(value = SUBJECT_HEADER, required = false) String actorUserId,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        "Store created",
                        fulfillmentNetworkService.createAdminStore(payload, actorUserId),
                        request.getRequestURI()
                ));
    }

    @PatchMapping("/api/v1/admin/stores/{storeId}/status")
    public ApiResponse<StoreResponse> updateStoreStatus(
            @PathVariable UUID storeId,
            @Valid @RequestBody StoreStatusUpdateRequest payload,
            @RequestHeader(value = SUBJECT_HEADER, required = false) String actorUserId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Store status updated",
                fulfillmentNetworkService.updateStoreStatus(storeId, payload, actorUserId),
                request.getRequestURI()
        );
    }

    @GetMapping("/api/v1/admin/stores/{storeId}/location")
    public ApiResponse<StoreLocationResponse> getStoreLocation(
            @PathVariable UUID storeId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Store location",
                fulfillmentNetworkService.getStoreLocation(storeId),
                request.getRequestURI()
        );
    }

    @PutMapping("/api/v1/admin/stores/{storeId}/location")
    public ApiResponse<StoreLocationResponse> updateStoreLocation(
            @PathVariable UUID storeId,
            @RequestBody StoreLocationUpdateRequest payload,
            @RequestHeader(value = SUBJECT_HEADER, required = false) String actorUserId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Store location updated",
                fulfillmentNetworkService.updateStoreLocation(storeId, payload, actorUserId),
                request.getRequestURI()
        );
    }

    @GetMapping("/api/v1/stores")
    public ApiResponse<PageResponse<StoreResponse>> listStores(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) Boolean openNow,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Stores",
                fulfillmentNetworkService.listStores(service, openNow, page, size, sortBy, direction),
                request.getRequestURI()
        );
    }

    @GetMapping("/api/v1/stores/nearest")
    public ApiResponse<List<StoreResponse>> nearestStores(
            @RequestParam("lat") @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
            @RequestParam("lng") @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
            @RequestParam(defaultValue = "5") @Min(1) @Max(25) int limit,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Nearest stores",
                fulfillmentNetworkService.findNearestStores(latitude, longitude, limit),
                request.getRequestURI()
        );
    }

    @PutMapping("/api/v1/stores/preferred/{storeId}")
    public ApiResponse<Void> setPreferredStore(
            @PathVariable UUID storeId,
            @RequestHeader(value = SUBJECT_HEADER, required = false) String actorUserId,
            HttpServletRequest request
    ) {
        fulfillmentNetworkService.setPreferredStore(storeId, actorUserId);
        return ApiResponse.ok("Preferred store updated", null, request.getRequestURI());
    }

    @PostMapping("/api/v1/stores")
    public ResponseEntity<ApiResponse<StoreResponse>> createStore(
            @Valid @RequestBody StoreRequest payload,
            @RequestHeader(value = SUBJECT_HEADER, required = false) String actorUserId,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        "Store created",
                        fulfillmentNetworkService.createStore(payload, actorUserId),
                        request.getRequestURI()
                ));
    }

    @PutMapping("/api/v1/stores/{storeId}")
    public ApiResponse<StoreResponse> updateStore(
            @PathVariable UUID storeId,
            @Valid @RequestBody StoreRequest payload,
            @RequestHeader(value = SUBJECT_HEADER, required = false) String actorUserId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Store updated",
                fulfillmentNetworkService.updateStore(storeId, payload, actorUserId),
                request.getRequestURI()
        );
    }

    @DeleteMapping("/api/v1/stores/{storeId}")
    public ApiResponse<Void> deleteStore(
            @PathVariable UUID storeId,
            @RequestHeader(value = SUBJECT_HEADER, required = false) String actorUserId,
            HttpServletRequest request
    ) {
        fulfillmentNetworkService.deleteStore(storeId, actorUserId);
        return ApiResponse.ok("Store moved to trash", null, request.getRequestURI());
    }

    @GetMapping("/api/v1/admin/service-areas")
    public ApiResponse<PageResponse<ServiceAreaResponse>> listServiceAreas(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) ServiceAreaStatus status,
            @RequestParam(required = false) ServiceAreaType type,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Service areas",
                fulfillmentNetworkService.listServiceAreas(query, status, type, page, size, sortBy, direction),
                request.getRequestURI()
        );
    }

    @GetMapping("/api/v1/admin/service-areas/{serviceAreaId}")
    public ApiResponse<ServiceAreaResponse> getServiceArea(
            @PathVariable UUID serviceAreaId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok("Service area", fulfillmentNetworkService.getServiceArea(serviceAreaId), request.getRequestURI());
    }

    @PostMapping("/api/v1/admin/service-areas")
    public ResponseEntity<ApiResponse<ServiceAreaResponse>> createServiceArea(
            @Valid @RequestBody ServiceAreaRequest payload,
            @RequestHeader(value = SUBJECT_HEADER, required = false) String actorUserId,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        "Service area created",
                        fulfillmentNetworkService.createServiceArea(payload, actorUserId),
                        request.getRequestURI()
                ));
    }

    @PutMapping("/api/v1/admin/service-areas/{serviceAreaId}")
    public ApiResponse<ServiceAreaResponse> updateServiceArea(
            @PathVariable UUID serviceAreaId,
            @Valid @RequestBody ServiceAreaRequest payload,
            @RequestHeader(value = SUBJECT_HEADER, required = false) String actorUserId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Service area updated",
                fulfillmentNetworkService.updateServiceArea(serviceAreaId, payload, actorUserId),
                request.getRequestURI()
        );
    }

    @DeleteMapping("/api/v1/admin/service-areas/{serviceAreaId}")
    public ApiResponse<Void> deleteServiceArea(
            @PathVariable UUID serviceAreaId,
            @RequestHeader(value = SUBJECT_HEADER, required = false) String actorUserId,
            HttpServletRequest request
    ) {
        fulfillmentNetworkService.deleteServiceArea(serviceAreaId, actorUserId);
        return ApiResponse.ok("Service area moved to trash", null, request.getRequestURI());
    }

    @PostMapping("/api/v1/admin/service-areas/{serviceAreaId}/activate")
    public ApiResponse<ServiceAreaResponse> activateServiceArea(
            @PathVariable UUID serviceAreaId,
            @RequestHeader(value = SUBJECT_HEADER, required = false) String actorUserId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Service area activated",
                fulfillmentNetworkService.activateServiceArea(serviceAreaId, actorUserId),
                request.getRequestURI()
        );
    }

    @PostMapping("/api/v1/admin/service-areas/{serviceAreaId}/deactivate")
    public ApiResponse<ServiceAreaResponse> deactivateServiceArea(
            @PathVariable UUID serviceAreaId,
            @RequestHeader(value = SUBJECT_HEADER, required = false) String actorUserId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Service area deactivated",
                fulfillmentNetworkService.deactivateServiceArea(serviceAreaId, actorUserId),
                request.getRequestURI()
        );
    }

    @PostMapping("/api/v1/admin/service-areas/validate")
    public ApiResponse<ServiceEligibilityResponse> validateServiceArea(
            @Valid @RequestBody ServiceAreaValidationRequest payload,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Service eligibility",
                fulfillmentNetworkService.validateServiceArea(payload),
                request.getRequestURI()
        );
    }
}
