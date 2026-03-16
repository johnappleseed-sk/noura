package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.common.api.PaginationUtils;
import com.noura.platform.domain.enums.StoreStatus;
import com.noura.platform.domain.enums.StoreType;
import com.noura.platform.dto.store.CreateStoreRequest;
import com.noura.platform.dto.store.StoreResponse;
import com.noura.platform.dto.store.UpdateStoreStatusRequest;
import com.noura.platform.service.StoreService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/admin/stores")
public class StoreAdminController {

    private final StoreService storeService;

    @PostMapping
    public ResponseEntity<ApiResponse<StoreResponse>> create(
            @Valid @RequestBody CreateStoreRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Store created", storeService.createAdminStore(request), http.getRequestURI()));
    }

    @GetMapping
    public ApiResponse<PageResponse<StoreResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID merchantId,
            @RequestParam(required = false) StoreType type,
            @RequestParam(required = false) StoreStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = PaginationUtils.pageOf(page, size, sortBy, direction);
        Page<StoreResponse> stores = storeService.listAdminStores(search, merchantId, type, status, pageable);
        return ApiResponse.ok("Stores", PageResponse.from(stores), http.getRequestURI());
    }

    @GetMapping("/{storeId}")
    public ApiResponse<StoreResponse> get(@PathVariable UUID storeId, HttpServletRequest http) {
        return ApiResponse.ok("Store", storeService.getAdminStore(storeId), http.getRequestURI());
    }

    @PatchMapping("/{storeId}/status")
    public ApiResponse<StoreResponse> updateStatus(
            @PathVariable UUID storeId,
            @Valid @RequestBody UpdateStoreStatusRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Store status updated", storeService.updateStoreStatus(storeId, request), http.getRequestURI());
    }
}
