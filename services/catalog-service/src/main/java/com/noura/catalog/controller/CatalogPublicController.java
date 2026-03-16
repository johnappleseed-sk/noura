package com.noura.catalog.controller;

import com.noura.catalog.common.ApiResponse;
import com.noura.catalog.common.PageResponse;
import com.noura.catalog.dto.catalog.CategoryTreeDto;
import com.noura.catalog.dto.product.ProductDto;
import com.noura.catalog.dto.product.ProductSearchResultDto;
import com.noura.catalog.dto.product.ProductStoreInventoryDto;
import com.noura.catalog.dto.product.TrendTagDto;
import com.noura.catalog.domain.enums.ProductStatus;
import com.noura.catalog.service.CatalogQueryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}")
public class CatalogPublicController {

    private final CatalogQueryService catalogQueryService;

    @GetMapping("/products")
    public ApiResponse<PageResponse<ProductDto>> listProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Boolean flashSale,
            @RequestParam(required = false) Boolean trending,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(parseDirection(direction), mapSortField(sortBy)));
        Page<ProductDto> data = catalogQueryService.listProducts(
                query,
                category,
                categoryId,
                brand,
                minPrice,
                maxPrice,
                minRating,
                flashSale,
                trending,
                pageable
        );
        return ApiResponse.ok("Products", PageResponse.from(data), http.getRequestURI());
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<ProductDto> getProduct(@PathVariable UUID productId, HttpServletRequest http) {
        return ApiResponse.ok("Product", catalogQueryService.getProduct(productId), http.getRequestURI());
    }

    @GetMapping("/products/{productId}/inventory")
    public ApiResponse<List<ProductStoreInventoryDto>> productInventory(@PathVariable UUID productId, HttpServletRequest http) {
        return ApiResponse.ok("Store inventory", catalogQueryService.productInventory(productId), http.getRequestURI());
    }

    @GetMapping("/products/search")
    public ApiResponse<List<ProductSearchResultDto>> searchProducts(
            @RequestParam("q") String query,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Product search results", catalogQueryService.searchProducts(query), http.getRequestURI());
    }

    @GetMapping("/admin/products/search")
    public ApiResponse<PageResponse<ProductSearchResultDto>> searchProductsForAdmin(
            @RequestParam(required = false, name = "q") String q,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(parseDirection(direction), mapSortField(sortBy)));
        Page<ProductSearchResultDto> results = catalogQueryService.searchProducts(
                coalesceKeyword(keyword, q),
                categoryId,
                brandId,
                status,
                pageable
        );
        return ApiResponse.ok("Admin product search results", PageResponse.from(results), http.getRequestURI());
    }

    private String coalesceKeyword(String keyword, String q) {
        return keyword != null && !keyword.isBlank() ? keyword : q;
    }

    @GetMapping("/products/trend-tags")
    public ApiResponse<List<TrendTagDto>> trendTags(HttpServletRequest http) {
        return ApiResponse.ok("Trend tags", catalogQueryService.trendTags(), http.getRequestURI());
    }

    @GetMapping("/categories/tree")
    public ApiResponse<List<CategoryTreeDto>> categoryTree(
            @RequestParam(required = false) String locale,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Category tree", catalogQueryService.categoryTree(), http.getRequestURI());
    }

    private Sort.Direction parseDirection(String raw) {
        return "asc".equalsIgnoreCase(raw) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    private String mapSortField(String raw) {
        if (raw == null || raw.isBlank()) {
            return "createdAt";
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "name" -> "name";
            case "productcode" -> "productCode";
            case "price", "baseprice" -> "basePrice";
            case "status" -> "status";
            case "updatedat" -> "updatedAt";
            case "popular", "popularityscore" -> "popularityScore";
            default -> "createdAt";
        };
    }
}
