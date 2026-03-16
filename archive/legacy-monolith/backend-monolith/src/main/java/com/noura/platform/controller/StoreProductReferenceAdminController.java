package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.common.api.PaginationUtils;
import com.noura.platform.dto.superinventory.StoreProductReferenceResponse;
import com.noura.platform.service.SuperInventoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping({"/api/admin/stores", "${app.api.version-prefix:/api/v1}/admin/stores"})
public class StoreProductReferenceAdminController {

    private final SuperInventoryService superInventoryService;

    @GetMapping("/{storeId}/product-references")
    public ApiResponse<PageResponse<StoreProductReferenceResponse>> list(
            @PathVariable UUID storeId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = PaginationUtils.pageOf(page, size, sortBy, direction);
        Page<StoreProductReferenceResponse> references = superInventoryService
                .listStoreProductReferences(storeId, productId, active, pageable);
        return ApiResponse.ok("Store product references", PageResponse.from(references), http.getRequestURI());
    }

    @PostMapping("/{storeId}/products/{productId}/link")
    public ResponseEntity<ApiResponse<StoreProductReferenceResponse>> linkProduct(
            @PathVariable UUID storeId,
            @PathVariable UUID productId,
            HttpServletRequest http
    ) {
        StoreProductReferenceResponse response = superInventoryService.linkStoreProduct(storeId, productId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Store product linked", response, http.getRequestURI()));
    }
}
