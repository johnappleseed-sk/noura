package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.domain.enums.ProductStatus;
import com.noura.platform.dto.product.ProductSearchRequest;
import com.noura.platform.dto.product.ProductSearchResponse;
import com.noura.platform.service.ProductSearchService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    @GetMapping("/products/search")
    public ApiResponse<PageResponse<ProductSearchResponse>> searchProducts(
            @RequestParam(required = false, name = "q") String q,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        ProductSearchRequest request = new ProductSearchRequest(
                coalesceKeyword(keyword, q),
                categoryId,
                brandId,
                status,
                page,
                size,
                sortBy,
                direction
        );
        Page<ProductSearchResponse> results = productSearchService.searchPublic(request);
        return ApiResponse.ok("Product search results", PageResponse.from(results), http.getRequestURI());
    }

    @GetMapping("/admin/products/search")
    public ApiResponse<PageResponse<ProductSearchResponse>> searchAdminProducts(
            @RequestParam(required = false, name = "q") String q,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        ProductSearchRequest request = new ProductSearchRequest(
                coalesceKeyword(keyword, q),
                categoryId,
                brandId,
                status,
                page,
                size,
                sortBy,
                direction
        );
        Page<ProductSearchResponse> results = productSearchService.searchAdmin(request);
        return ApiResponse.ok("Admin product search results", PageResponse.from(results), http.getRequestURI());
    }

    private String coalesceKeyword(String keyword, String q) {
        if (keyword != null && !keyword.isBlank()) {
            return keyword;
        }
        return q;
    }
}
