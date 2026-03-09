package com.noura.platform.service;

import com.noura.platform.commerce.api.v1.dto.inventory.StockAvailabilityDto;
import com.noura.platform.commerce.api.v1.dto.product.ApiProductDto;
import com.noura.platform.commerce.api.v1.dto.product.ApiProductUnitDto;
import com.noura.platform.commerce.catalog.web.StorefrontCategoryDto;
import com.noura.platform.commerce.catalog.web.StorefrontProductCardDto;
import com.noura.platform.commerce.catalog.web.StorefrontProductDetailDto;
import com.noura.platform.dto.catalog.AttributeDto;
import com.noura.platform.dto.catalog.AttributeRequest;
import com.noura.platform.dto.catalog.AttributeSetDto;
import com.noura.platform.dto.catalog.AttributeSetRequest;
import com.noura.platform.dto.catalog.CategoryAnalyticsDto;
import com.noura.platform.dto.catalog.CategoryChangeRequestDto;
import com.noura.platform.dto.catalog.CategoryChangeReviewRequest;
import com.noura.platform.dto.catalog.CategoryChangeSubmitRequest;
import com.noura.platform.dto.catalog.CategoryDto;
import com.noura.platform.dto.catalog.CategoryRequest;
import com.noura.platform.dto.catalog.CategorySuggestionRequest;
import com.noura.platform.dto.catalog.CategorySuggestionResponse;
import com.noura.platform.dto.catalog.CategoryTranslationDto;
import com.noura.platform.dto.catalog.CategoryTranslationRequest;
import com.noura.platform.dto.catalog.CategoryTreeDto;
import com.noura.platform.dto.catalog.CategoryUpdateRequest;
import com.noura.platform.dto.catalog.ChannelCategoryMappingDto;
import com.noura.platform.dto.catalog.ChannelCategoryMappingRequest;
import com.noura.platform.dto.product.ProductDto;
import com.noura.platform.dto.product.ProductFilterRequest;
import com.noura.platform.dto.product.ProductInventoryDto;
import com.noura.platform.dto.product.ProductInventoryRequest;
import com.noura.platform.dto.product.ProductMediaDto;
import com.noura.platform.dto.product.ProductMediaRequest;
import com.noura.platform.dto.product.ProductPatchRequest;
import com.noura.platform.dto.product.ProductRequest;
import com.noura.platform.dto.product.ProductReviewDto;
import com.noura.platform.dto.product.ProductReviewRequest;
import com.noura.platform.dto.product.ProductVariantDto;
import com.noura.platform.dto.product.ProductVariantRequest;
import com.noura.platform.dto.product.TrendTagDto;
import com.noura.platform.domain.enums.CategoryChangeRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UnifiedCatalogService {
    Page<ProductDto> listProducts(ProductFilterRequest filter, Pageable pageable);

    List<TrendTagDto> trendTags();

    ProductDto getProduct(UUID productId);

    ProductDto createProduct(ProductRequest request);

    ProductDto updateProduct(UUID productId, ProductRequest request);

    ProductDto patchProduct(UUID productId, ProductPatchRequest request);

    void deleteProduct(UUID productId);

    List<ProductReviewDto> reviews(UUID productId);

    ProductReviewDto addReview(UUID productId, ProductReviewRequest request);

    ProductVariantDto addVariant(UUID productId, ProductVariantRequest request);

    List<ProductVariantDto> listVariants(UUID productId);

    ProductVariantDto updateVariant(UUID variantId, ProductVariantRequest request);

    ProductMediaDto addMedia(UUID productId, ProductMediaRequest request);

    ProductInventoryDto upsertInventory(UUID productId, ProductInventoryRequest request);

    List<ProductInventoryDto> inventories(UUID productId);

    List<ProductDto> relatedProducts(UUID productId);

    List<ProductDto> frequentlyBoughtTogether(UUID productId);

    CategoryDto createCategory(CategoryRequest request);

    List<CategoryTreeDto> categoryTree(String locale);

    CategoryDto updateCategory(UUID categoryId, CategoryUpdateRequest request);

    AttributeDto createAttribute(AttributeRequest request);

    AttributeSetDto createAttributeSet(AttributeSetRequest request);

    CategoryTranslationDto upsertCategoryTranslation(UUID categoryId, String locale, CategoryTranslationRequest request);

    List<CategoryTranslationDto> categoryTranslations(UUID categoryId);

    ChannelCategoryMappingDto createChannelCategoryMapping(ChannelCategoryMappingRequest request);

    List<ChannelCategoryMappingDto> categoryChannelMappings(UUID categoryId);

    CategoryChangeRequestDto submitCategoryChangeRequest(CategoryChangeSubmitRequest request);

    List<CategoryChangeRequestDto> categoryChangeRequests(CategoryChangeRequestStatus status);

    CategoryChangeRequestDto approveCategoryChangeRequest(UUID requestId, CategoryChangeReviewRequest request);

    CategoryChangeRequestDto rejectCategoryChangeRequest(UUID requestId, CategoryChangeReviewRequest request);

    CategorySuggestionResponse suggestCategory(CategorySuggestionRequest request);

    List<CategoryAnalyticsDto> categoryAnalytics(Instant from, Instant to);

    Page<ApiProductDto> listCommerceProducts(String q, Long categoryId, Boolean active, Boolean lowStock, Pageable pageable);

    ApiProductDto getCommerceProductById(Long id);

    ApiProductDto createCommerceProduct(com.noura.platform.commerce.api.v1.dto.product.ProductCreateRequest request);

    ApiProductDto updateCommerceProduct(Long id, com.noura.platform.commerce.api.v1.dto.product.ProductUpdateRequest request);

    List<ApiProductUnitDto> listCommerceProductUnits(Long productId);

    ApiProductUnitDto createCommerceProductUnit(Long productId,
                                                com.noura.platform.commerce.api.v1.dto.product.ProductUnitUpsertRequest request);

    ApiProductUnitDto updateCommerceProductUnit(Long productId,
                                                Long unitId,
                                                com.noura.platform.commerce.api.v1.dto.product.ProductUnitUpsertRequest request);

    void deleteCommerceProductUnit(Long productId, Long unitId);

    // ── Storefront catalog ──────────────────────────────────────────────

    List<StorefrontCategoryDto> listStorefrontCategories();

    Page<StorefrontProductCardDto> listStorefrontProducts(String q, Long categoryId, Pageable pageable);

    StorefrontProductDetailDto getStorefrontProduct(Long productId);

    StockAvailabilityDto getStorefrontProductAvailability(Long productId);
}
