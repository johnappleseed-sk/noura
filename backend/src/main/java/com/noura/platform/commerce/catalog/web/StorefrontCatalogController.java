package com.noura.platform.commerce.catalog.web;

import com.noura.platform.commerce.api.v1.dto.common.ApiEnvelope;
import com.noura.platform.commerce.api.v1.dto.common.ApiPageData;
import com.noura.platform.commerce.api.v1.dto.inventory.StockAvailabilityDto;
import com.noura.platform.commerce.api.v1.support.ApiTrace;
import com.noura.platform.commerce.catalog.application.StorefrontCatalogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/storefront/v1/catalog")
public class StorefrontCatalogController {
    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 48;

    private final StorefrontCatalogService storefrontCatalogService;

    public StorefrontCatalogController(StorefrontCatalogService storefrontCatalogService) {
        this.storefrontCatalogService = storefrontCatalogService;
    }

    @GetMapping("/categories")
    public ApiEnvelope<List<StorefrontCategoryDto>> categories(HttpServletRequest request) {
        return ApiEnvelope.success(
                "STOREFRONT_CATEGORY_LIST_OK",
                "Storefront categories fetched successfully.",
                storefrontCatalogService.listCategories(),
                ApiTrace.resolve(request)
        );
    }

    @GetMapping("/products")
    public ApiEnvelope<ApiPageData<StorefrontProductCardDto>> products(@RequestParam(required = false) String q,
                                                                       @RequestParam(required = false) Long categoryId,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "12") int size,
                                                                       @RequestParam(defaultValue = "featured") String sort,
                                                                       HttpServletRequest request) {
        Page<StorefrontProductCardDto> productPage = storefrontCatalogService.listProducts(
                q,
                categoryId,
                PageRequest.of(Math.max(0, page), normalizePageSize(size), sortBy(sort))
        );
        return ApiEnvelope.success(
                "STOREFRONT_PRODUCT_LIST_OK",
                "Storefront products fetched successfully.",
                ApiPageData.from(productPage),
                ApiTrace.resolve(request)
        );
    }

    @GetMapping("/products/{id}")
    public ApiEnvelope<StorefrontProductDetailDto> product(@PathVariable Long id, HttpServletRequest request) {
        return ApiEnvelope.success(
                "STOREFRONT_PRODUCT_FETCH_OK",
                "Storefront product fetched successfully.",
                storefrontCatalogService.getProduct(id),
                ApiTrace.resolve(request)
        );
    }

    @GetMapping("/products/{id}/availability")
    public ApiEnvelope<StockAvailabilityDto> availability(@PathVariable Long id, HttpServletRequest request) {
        return ApiEnvelope.success(
                "STOREFRONT_PRODUCT_AVAILABILITY_OK",
                "Storefront product availability fetched successfully.",
                storefrontCatalogService.getAvailability(id),
                ApiTrace.resolve(request)
        );
    }

    private Sort sortBy(String sort) {
        return switch (sort == null ? "" : sort.trim()) {
            case "name" -> Sort.by(Sort.Order.asc("name"), Sort.Order.desc("id"));
            case "priceAsc" -> Sort.by(Sort.Order.asc("price"), Sort.Order.desc("id"));
            case "priceDesc" -> Sort.by(Sort.Order.desc("price"), Sort.Order.desc("id"));
            default -> Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"));
        };
    }

    private int normalizePageSize(int requested) {
        if (requested <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(requested, MAX_PAGE_SIZE);
    }
}
