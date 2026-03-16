package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.contract.StoreStaffAssignmentDto;
import com.noura.platform.dto.contract.StoreStaffAssignmentRequest;
import com.noura.platform.service.MerchantContractService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/admin/stores/{storeId}/staff")
public class StoreStaffAdminController {

    private final MerchantContractService merchantContractService;

    @GetMapping
    public ApiResponse<List<StoreStaffAssignmentDto>> list(@PathVariable UUID storeId, HttpServletRequest http) {
        return ApiResponse.ok("Store staff", merchantContractService.listStoreAssignments(storeId), http.getRequestURI());
    }

    @PutMapping
    public ResponseEntity<ApiResponse<StoreStaffAssignmentDto>> upsert(
            @PathVariable UUID storeId,
            @Valid @RequestBody StoreStaffAssignmentRequest request,
            HttpServletRequest http
    ) {
        StoreStaffAssignmentDto saved = merchantContractService.upsertStoreAssignment(storeId, request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok("Store staff assignment saved", saved, http.getRequestURI()));
    }
}

