package com.noura.platform.service;

import com.noura.platform.dto.product.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    /**
     * Creates product.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    ProductDto createProduct(ProductRequest request);

    /**
     * Updates product.
     *
     * @param productId The product id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    ProductDto updateProduct(UUID productId, ProductRequest request);

    /**
     * Patches product.
     *
     * @param productId The product id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    default ProductDto patchProduct(UUID productId, ProductPatchRequest request) {
        throw new UnsupportedOperationException("Patch product not implemented");
    }

    /**
     * Deletes product.
     *
     * @param productId The product id used to locate the target record.
     */
    void deleteProduct(UUID productId);

    /**
     * Retrieves product.
     *
     * @param productId The product id used to locate the target record.
     * @return The mapped DTO representation.
     */
    ProductDto getProduct(UUID productId);

    /**
     * Lists products.
     *
     * @param filter The filter criteria applied to this operation.
     * @param pageable The pagination configuration.
     * @return A paginated result set.
     */
    Page<ProductDto> listProducts(ProductFilterRequest filter, Pageable pageable);

    /**
     * Lists variants.
     *
     * @param productId The product id used to locate the target record.
     * @return A list of matching items.
     */
    List<ProductVariantDto> listVariants(UUID productId);

    /**
     * Adds variant.
     *
     * @param productId The product id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    ProductVariantDto addVariant(UUID productId, ProductVariantRequest request);

    /**
     * Updates variant.
     *
     * @param variantId The variant id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    ProductVariantDto updateVariant(UUID variantId, ProductVariantRequest request);

    /**
     * Adds media.
     *
     * @param productId The product id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    ProductMediaDto addMedia(UUID productId, ProductMediaRequest request);

    /**
     * Executes upsert inventory.
     *
     * @param productId The product id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    ProductInventoryDto upsertInventory(UUID productId, ProductInventoryRequest request);

    /**
     * Executes inventories.
     *
     * @param productId The product id used to locate the target record.
     * @return A list of matching items.
     */
    List<ProductInventoryDto> inventories(UUID productId);

    /**
     * Adds review.
     *
     * @param productId The product id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    ProductReviewDto addReview(UUID productId, ProductReviewRequest request);

    /**
     * Executes reviews.
     *
     * @param productId The product id used to locate the target record.
     * @return A list of matching items.
     */
    List<ProductReviewDto> reviews(UUID productId);

    /**
     * Executes related products.
     *
     * @param productId The product id used to locate the target record.
     * @return A list of matching items.
     */
    List<ProductDto> relatedProducts(UUID productId);

    /**
     * Executes frequently bought together.
     *
     * @param productId The product id used to locate the target record.
     * @return A list of matching items.
     */
    List<ProductDto> frequentlyBoughtTogether(UUID productId);

    /**
     * Executes trend tags.
     *
     * @return A list of matching items.
     */
    List<TrendTagDto> trendTags();
}
