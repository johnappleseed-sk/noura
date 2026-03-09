package com.noura.platform.service.impl;

import com.noura.platform.commerce.api.v1.dto.inventory.StockAvailabilityDto;
import com.noura.platform.commerce.api.v1.dto.product.ApiProductDto;
import com.noura.platform.commerce.api.v1.dto.product.ApiProductUnitDto;
import com.noura.platform.commerce.api.v1.service.ApiProductService;
import com.noura.platform.commerce.catalog.application.StorefrontCatalogService;
import com.noura.platform.commerce.catalog.web.StorefrontCategoryDto;
import com.noura.platform.commerce.catalog.web.StorefrontProductCardDto;
import com.noura.platform.commerce.catalog.web.StorefrontProductDetailDto;
import com.noura.platform.domain.enums.CategoryChangeRequestStatus;
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
import com.noura.platform.service.CatalogManagementService;
import com.noura.platform.service.ProductService;
import com.noura.platform.service.UnifiedCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UnifiedCatalogServiceImpl implements UnifiedCatalogService {

    private final ProductService platformProductService;
    private final CatalogManagementService platformCatalogManagementService;
    private final ObjectProvider<ApiProductService> commerceProductServiceProvider;
    private final ObjectProvider<StorefrontCatalogService> storefrontCatalogServiceProvider;

    @Override
    public Page<ProductDto> listProducts(ProductFilterRequest filter, Pageable pageable) {
        return platformProductService.listProducts(filter, pageable);
    }

    @Override
    public List<TrendTagDto> trendTags() {
        return platformProductService.trendTags();
    }

    @Override
    public ProductDto getProduct(UUID productId) {
        return platformProductService.getProduct(productId);
    }

    @Override
    public ProductDto createProduct(ProductRequest request) {
        return platformProductService.createProduct(request);
    }

    @Override
    public ProductDto updateProduct(UUID productId, ProductRequest request) {
        return platformProductService.updateProduct(productId, request);
    }

    @Override
    public ProductDto patchProduct(UUID productId, ProductPatchRequest request) {
        return platformProductService.patchProduct(productId, request);
    }

    @Override
    public void deleteProduct(UUID productId) {
        platformProductService.deleteProduct(productId);
    }

    @Override
    public List<ProductReviewDto> reviews(UUID productId) {
        return platformProductService.reviews(productId);
    }

    @Override
    public ProductReviewDto addReview(UUID productId, ProductReviewRequest request) {
        return platformProductService.addReview(productId, request);
    }

    @Override
    public ProductVariantDto addVariant(UUID productId, ProductVariantRequest request) {
        return platformProductService.addVariant(productId, request);
    }

    @Override
    public List<ProductVariantDto> listVariants(UUID productId) {
        return platformProductService.listVariants(productId);
    }

    @Override
    public ProductVariantDto updateVariant(UUID variantId, ProductVariantRequest request) {
        return platformProductService.updateVariant(variantId, request);
    }

    @Override
    public ProductMediaDto addMedia(UUID productId, ProductMediaRequest request) {
        return platformProductService.addMedia(productId, request);
    }

    @Override
    public ProductInventoryDto upsertInventory(UUID productId, ProductInventoryRequest request) {
        return platformProductService.upsertInventory(productId, request);
    }

    @Override
    public List<ProductInventoryDto> inventories(UUID productId) {
        return platformProductService.inventories(productId);
    }

    @Override
    public List<ProductDto> relatedProducts(UUID productId) {
        return platformProductService.relatedProducts(productId);
    }

    @Override
    public List<ProductDto> frequentlyBoughtTogether(UUID productId) {
        return platformProductService.frequentlyBoughtTogether(productId);
    }

    @Override
    public CategoryDto createCategory(CategoryRequest request) {
        return platformCatalogManagementService.createCategory(request);
    }

    @Override
    public List<CategoryTreeDto> categoryTree(String locale) {
        return platformCatalogManagementService.categoryTree(locale);
    }

    @Override
    public CategoryDto updateCategory(UUID categoryId, CategoryUpdateRequest request) {
        return platformCatalogManagementService.updateCategory(categoryId, request);
    }

    @Override
    public AttributeDto createAttribute(AttributeRequest request) {
        return platformCatalogManagementService.createAttribute(request);
    }

    @Override
    public AttributeSetDto createAttributeSet(AttributeSetRequest request) {
        return platformCatalogManagementService.createAttributeSet(request);
    }

    @Override
    public CategoryTranslationDto upsertCategoryTranslation(UUID categoryId, String locale, CategoryTranslationRequest request) {
        return platformCatalogManagementService.upsertCategoryTranslation(categoryId, locale, request);
    }

    @Override
    public List<CategoryTranslationDto> categoryTranslations(UUID categoryId) {
        return platformCatalogManagementService.categoryTranslations(categoryId);
    }

    @Override
    public ChannelCategoryMappingDto createChannelCategoryMapping(ChannelCategoryMappingRequest request) {
        return platformCatalogManagementService.createChannelCategoryMapping(request);
    }

    @Override
    public List<ChannelCategoryMappingDto> categoryChannelMappings(UUID categoryId) {
        return platformCatalogManagementService.categoryChannelMappings(categoryId);
    }

    @Override
    public CategoryChangeRequestDto submitCategoryChangeRequest(CategoryChangeSubmitRequest request) {
        return platformCatalogManagementService.submitCategoryChangeRequest(request);
    }

    @Override
    public List<CategoryChangeRequestDto> categoryChangeRequests(CategoryChangeRequestStatus status) {
        return platformCatalogManagementService.categoryChangeRequests(status);
    }

    @Override
    public CategoryChangeRequestDto approveCategoryChangeRequest(UUID requestId, CategoryChangeReviewRequest request) {
        return platformCatalogManagementService.approveCategoryChangeRequest(requestId, request);
    }

    @Override
    public CategoryChangeRequestDto rejectCategoryChangeRequest(UUID requestId, CategoryChangeReviewRequest request) {
        return platformCatalogManagementService.rejectCategoryChangeRequest(requestId, request);
    }

    @Override
    public CategorySuggestionResponse suggestCategory(CategorySuggestionRequest request) {
        return platformCatalogManagementService.suggestCategory(request);
    }

    @Override
    public List<CategoryAnalyticsDto> categoryAnalytics(Instant from, Instant to) {
        return platformCatalogManagementService.categoryAnalytics(from, to);
    }

    @Override
    public Page<ApiProductDto> listCommerceProducts(String q, Long categoryId, Boolean active, Boolean lowStock, Pageable pageable) {
        return commerceProductService().list(q, categoryId, active, lowStock, pageable);
    }

    @Override
    public ApiProductDto getCommerceProductById(Long id) {
        return commerceProductService().getById(id);
    }

    @Override
    public ApiProductDto createCommerceProduct(
            com.noura.platform.commerce.api.v1.dto.product.ProductCreateRequest request
    ) {
        return commerceProductService().create(request);
    }

    @Override
    public ApiProductDto updateCommerceProduct(
            Long id,
            com.noura.platform.commerce.api.v1.dto.product.ProductUpdateRequest request
    ) {
        return commerceProductService().update(id, request);
    }

    @Override
    public List<ApiProductUnitDto> listCommerceProductUnits(Long productId) {
        return commerceProductService().listUnits(productId);
    }

    @Override
    public ApiProductUnitDto createCommerceProductUnit(
            Long productId,
            com.noura.platform.commerce.api.v1.dto.product.ProductUnitUpsertRequest request
    ) {
        return commerceProductService().createUnit(productId, request);
    }

    @Override
    public ApiProductUnitDto updateCommerceProductUnit(
            Long productId,
            Long unitId,
            com.noura.platform.commerce.api.v1.dto.product.ProductUnitUpsertRequest request
    ) {
        return commerceProductService().updateUnit(productId, unitId, request);
    }

    @Override
    public void deleteCommerceProductUnit(Long productId, Long unitId) {
        commerceProductService().deleteUnit(productId, unitId);
    }

    private ApiProductService commerceProductService() {
        ApiProductService service = commerceProductServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("Legacy commerce product service is not active in the current runtime profile.");
        }
        return service;
    }

    private StorefrontCatalogService storefrontCatalogService() {
        StorefrontCatalogService service = storefrontCatalogServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("Storefront catalog service is not active in the current runtime profile.");
        }
        return service;
    }

    // ── Storefront catalog ──────────────────────────────────────────────

    @Override
    public List<StorefrontCategoryDto> listStorefrontCategories() {
        return storefrontCatalogService().listCategories();
    }

    @Override
    public Page<StorefrontProductCardDto> listStorefrontProducts(String q, Long categoryId, Pageable pageable) {
        return storefrontCatalogService().listProducts(q, categoryId, pageable);
    }

    @Override
    public StorefrontProductDetailDto getStorefrontProduct(Long productId) {
        return storefrontCatalogService().getProduct(productId);
    }

    @Override
    public StockAvailabilityDto getStorefrontProductAvailability(Long productId) {
        return storefrontCatalogService().getAvailability(productId);
    }
}
