package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.common.api.PaginationUtils;
import com.noura.platform.domain.enums.MerchantStatus;
import com.noura.platform.dto.contract.MerchantContractCreateRequest;
import com.noura.platform.dto.contract.MerchantContractDto;
import com.noura.platform.dto.merchant.CreateMerchantRequest;
import com.noura.platform.dto.merchant.MerchantResponse;
import com.noura.platform.dto.merchant.UpdateMerchantStatusRequest;
import com.noura.platform.service.MerchantContractService;
import com.noura.platform.service.MerchantService;
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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/admin/merchants")
public class MerchantAdminController {

    private final MerchantContractService merchantContractService;
    private final MerchantService merchantService;

    @GetMapping
    public ApiResponse<PageResponse<MerchantResponse>> listMerchants(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) MerchantStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = PaginationUtils.pageOf(page, size, sortBy, direction);
        String resolvedSearch = query != null && !query.isBlank() ? query : search;
        Page<MerchantResponse> merchants = merchantService.listMerchants(resolvedSearch, status, pageable);
        return ApiResponse.ok("Merchants", PageResponse.from(merchants), http.getRequestURI());
    }

    @GetMapping("/{merchantId}")
    public ApiResponse<MerchantResponse> getMerchant(@PathVariable UUID merchantId, HttpServletRequest http) {
        return ApiResponse.ok("Merchant", merchantService.getMerchant(merchantId), http.getRequestURI());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MerchantResponse>> createMerchant(
            @Valid @RequestBody CreateMerchantRequest request,
            HttpServletRequest http
    ) {
        MerchantResponse created = merchantService.createMerchant(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Merchant created", created, http.getRequestURI()));
    }

    @PatchMapping("/{merchantId}/status")
    public ApiResponse<MerchantResponse> updateStatus(
            @PathVariable UUID merchantId,
            @Valid @RequestBody UpdateMerchantStatusRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Merchant status updated", merchantService.updateMerchantStatus(merchantId, request), http.getRequestURI());
    }

    @PostMapping("/{merchantId}/contracts")
    public ResponseEntity<ApiResponse<MerchantContractDto>> createContract(
            @PathVariable UUID merchantId,
            @Valid @RequestBody MerchantContractCreateRequest request,
            HttpServletRequest http
    ) {
        MerchantContractDto created = merchantContractService.createContract(merchantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Contract created", created, http.getRequestURI()));
    }
}
