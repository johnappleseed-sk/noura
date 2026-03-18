package com.noura.catalog.service;

import com.noura.catalog.dto.catalog.CategoryTreeDto;
import com.noura.catalog.dto.product.ProductDto;
import com.noura.catalog.dto.product.ProductSearchResultDto;
import com.noura.catalog.dto.product.ProductStoreInventoryDto;
import com.noura.catalog.dto.product.TrendTagDto;
import com.noura.catalog.dto.product.VariantLookupResponse;
import com.noura.catalog.domain.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CatalogQueryService {

    Page<ProductDto> listProducts(
            String query,
            String category,
            UUID categoryId,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double minRating,
            Boolean flashSale,
            Boolean trending,
            Pageable pageable
    );

    ProductDto getProduct(UUID productId);

    List<ProductStoreInventoryDto> productInventory(UUID productId);

    List<CategoryTreeDto> categoryTree();

    List<ProductSearchResultDto> searchProducts(String query);

    Page<ProductSearchResultDto> searchProducts(
            String keyword,
            UUID categoryId,
            UUID brandId,
            ProductStatus status,
            Pageable pageable
    );

    List<TrendTagDto> trendTags();

    /**
     * Returns best-selling products for storefront recommendation rails.
     *
     * @param limit maximum number of items to return
     * @return recommendation list
     */
    List<ProductDto> bestSellerRecommendations(int limit);

    /**
     * Returns trending products for storefront recommendation rails.
     *
     * @param limit maximum number of items to return
     * @return recommendation list
     */
    List<ProductDto> trendingRecommendations(int limit);

    /**
     * Returns flash-sale or deal-oriented products for storefront recommendation rails.
     *
     * @param limit maximum number of items to return
     * @return recommendation list
     */
    List<ProductDto> dealRecommendations(int limit);

    /**
     * Returns deterministic personalized recommendations using the current catalog snapshot.
     *
     * @param customerRef optional customer subject or token fingerprint
     * @param limit maximum number of items to return
     * @return recommendation list
     */
    List<ProductDto> personalizedRecommendations(String customerRef, int limit);

    /**
     * Returns deterministic cross-sell recommendations using the current catalog snapshot.
     *
     * @param customerRef optional customer subject or token fingerprint
     * @param limit maximum number of items to return
     * @return recommendation list
     */
    List<ProductDto> crossSellRecommendations(String customerRef, int limit);

    /**
     * Returns category/brand-neighbor products for one product detail page.
     *
     * @param productId anchor product identifier
     * @param limit maximum number of items to return
     * @return related products
     */
    List<ProductDto> relatedProducts(UUID productId, int limit);

    /**
     * Returns deterministic "frequently bought together" suggestions for one product detail page.
     *
     * @param productId anchor product identifier
     * @param limit maximum number of items to return
     * @return recommended add-on products
     */
    List<ProductDto> frequentlyBoughtTogether(UUID productId, int limit);

    /**
     * Resolves one product variant to its owning product for internal compatibility adapters.
     *
     * @param variantId variant identifier
     * @return variant lookup response
     */
    VariantLookupResponse lookupVariant(UUID variantId);
}
