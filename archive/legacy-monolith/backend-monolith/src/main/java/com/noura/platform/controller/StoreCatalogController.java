package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.common.api.PaginationUtils;
import com.noura.platform.dto.product.ProductDto;
import com.noura.platform.dto.product.ProductInventoryDto;
import com.noura.platform.dto.product.StoreProductAdoptionRequest;
import com.noura.platform.service.StoreCatalogService;
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
@RequestMapping("${app.api.version-prefix:/api/v1}/stores/{storeId}")
public class StoreCatalogController {

    private final StoreCatalogService storeCatalogService;

    @GetMapping("/super-inventory")
    public ApiResponse<PageResponse<ProductDto>> searchSuperInventory(
            @PathVariable UUID storeId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = PaginationUtils.pageOf(page, size, sortBy, direction);
        Page<ProductDto> products = storeCatalogService.searchAdoptableMasterProducts(storeId, q, pageable);
        return ApiResponse.ok("Adoptable master products", PageResponse.from(products), http.getRequestURI());
    }

    @PostMapping("/super-inventory/{productId}/adopt")
    public ResponseEntity<ApiResponse<ProductInventoryDto>> adopt(
            @PathVariable UUID storeId,
            @PathVariable UUID productId,
            @Valid @RequestBody StoreProductAdoptionRequest request,
            HttpServletRequest http
    ) {
        ProductInventoryDto adopted = storeCatalogService.adoptMasterProduct(storeId, productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Master product adopted", adopted, http.getRequestURI()));
    }

    @PutMapping("/catalog/products/{productId}")
    public ApiResponse<ProductInventoryDto> updateStoreProduct(
            @PathVariable UUID storeId,
            @PathVariable UUID productId,
            @Valid @RequestBody StoreProductAdoptionRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Store product updated", storeCatalogService.updateStoreProduct(storeId, productId, request), http.getRequestURI());
    }
}

