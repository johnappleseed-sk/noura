package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.common.api.PaginationUtils;
import com.noura.platform.domain.enums.MerchantContractStatus;
import com.noura.platform.dto.contract.*;
import com.noura.platform.service.MerchantContractService;
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

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/admin/contracts")
public class ContractAdminController {

    private final MerchantContractService merchantContractService;

    @GetMapping
    public ApiResponse<PageResponse<MerchantContractDto>> listContracts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) MerchantContractStatus status,
            @RequestParam(required = false) UUID merchantId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = PaginationUtils.pageOf(page, size, sortBy, direction);
        Page<MerchantContractDto> contracts = merchantContractService.listContracts(query, status, merchantId, pageable);
        return ApiResponse.ok("Contracts", PageResponse.from(contracts), http.getRequestURI());
    }

    @GetMapping("/{contractId}")
    public ApiResponse<MerchantContractDto> getContract(@PathVariable UUID contractId, HttpServletRequest http) {
        return ApiResponse.ok("Contract", merchantContractService.getContract(contractId), http.getRequestURI());
    }

    @GetMapping("/{contractId}/actions")
    public ApiResponse<List<MerchantContractActionDto>> actions(@PathVariable UUID contractId, HttpServletRequest http) {
        return ApiResponse.ok("Contract actions", merchantContractService.contractActions(contractId), http.getRequestURI());
    }

    @PostMapping("/{contractId}/approve")
    public ApiResponse<MerchantContractDto> approve(
            @PathVariable UUID contractId,
            @RequestBody MerchantContractDecisionRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Contract approved", merchantContractService.approveContract(contractId, request), http.getRequestURI());
    }

    @PostMapping("/{contractId}/reject")
    public ApiResponse<MerchantContractDto> reject(
            @PathVariable UUID contractId,
            @RequestBody MerchantContractDecisionRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Contract rejected", merchantContractService.rejectContract(contractId, request), http.getRequestURI());
    }

    @PostMapping("/{contractId}/suspend")
    public ApiResponse<MerchantContractDto> suspend(
            @PathVariable UUID contractId,
            @RequestBody MerchantContractDecisionRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Contract suspended", merchantContractService.suspendContract(contractId, request), http.getRequestURI());
    }

    @PostMapping("/{contractId}/terminate")
    public ApiResponse<MerchantContractDto> terminate(
            @PathVariable UUID contractId,
            @RequestBody MerchantContractDecisionRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Contract terminated", merchantContractService.terminateContract(contractId, request), http.getRequestURI());
    }

    @PostMapping("/{contractId}/register-store")
    public ResponseEntity<ApiResponse<StoreTenantDto>> registerStore(
            @PathVariable UUID contractId,
            @Valid @RequestBody ContractStoreRegistrationRequest request,
            HttpServletRequest http
    ) {
        StoreTenantDto created = merchantContractService.registerStore(contractId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Store registered", created, http.getRequestURI()));
    }
}

