package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.common.api.PaginationUtils;
import com.noura.platform.dto.product.*;
import com.noura.platform.service.ProductService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/products")
public class ProductController {

    private final ProductService productService;

    /**
     * Lists products.
     *
     * @param query The search query text.
     * @param category The category value.
     * @param brand The brand value.
     * @param minPrice The min price value.
     * @param maxPrice The max price value.
     * @param minRating The min rating value.
     * @param storeId The store id used to locate the target record.
     * @param availableAtStore The available at store value.
     * @param flashSale The flash sale value.
     * @param trending The trending value.
     * @param view The view value.
     * @param page The pagination configuration.
     * @param size The size value.
     * @param sortBy The sort by value.
     * @param direction The direction value.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @GetMapping
    public ApiResponse<PageResponse<ProductDto>> listProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) Boolean availableAtStore,
            @RequestParam(required = false) Boolean flashSale,
            @RequestParam(required = false) Boolean trending,
            @RequestParam(required = false) String attributeKey,
            @RequestParam(required = false) String attributeValue,
            @RequestParam(required = false, defaultValue = "grid") String view,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        ProductFilterRequest filter = ProductFilterRequest.builder()
                .query(query)
                .category(category)
                .categoryId(categoryId)
                .brand(brand)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .minRating(minRating)
                .storeId(storeId)
                .availableAtStore(availableAtStore)
                .flashSale(flashSale)
                .trending(trending)
                .attributeKey(attributeKey)
                .attributeValue(attributeValue)
                .build();
        Pageable pageable = PaginationUtils.pageOf(page, size, sortBy, direction);
        Page<ProductDto> products = productService.listProducts(filter, pageable);
        return ApiResponse.ok("Products (" + view + " mode)", PageResponse.from(products), http.getRequestURI());
    }

    /**
     * Executes trend tags.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("/trend-tags")
    public ApiResponse<List<TrendTagDto>> trendTags(HttpServletRequest http) {
        return ApiResponse.ok("Trend tags", productService.trendTags(), http.getRequestURI());
    }

    /**
     * Retrieves product.
     *
     * @param productId The product id used to locate the target record.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @GetMapping("/{productId}")
    public ApiResponse<ProductDto> getProduct(@PathVariable UUID productId, HttpServletRequest http) {
        return ApiResponse.ok("Product", productService.getProduct(productId), http.getRequestURI());
    }

    /**
     * Creates resource.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductDto>> create(
            @Valid @RequestBody ProductRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product created", productService.createProduct(request), http.getRequestURI()));
    }

    /**
     * Updates resource.
     *
     * @param productId The product id used to locate the target record.
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PutMapping("/{productId}")
    public ApiResponse<ProductDto> update(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Product updated", productService.updateProduct(productId, request), http.getRequestURI());
    }

    /**
     * Patches resource.
     *
     * @param productId The product id used to locate the target record.
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PatchMapping("/{productId}")
    public ApiResponse<ProductDto> patch(
            @PathVariable UUID productId,
            @RequestBody ProductPatchRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Product patched", productService.patchProduct(productId, request), http.getRequestURI());
    }

    /**
     * Deletes resource.
     *
     * @param productId The product id used to locate the target record.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> delete(@PathVariable UUID productId, HttpServletRequest http) {
        productService.deleteProduct(productId);
        return ApiResponse.ok("Product deleted", null, http.getRequestURI());
    }

    /**
     * Executes reviews.
     *
     * @param productId The product id used to locate the target record.
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("/{productId}/reviews")
    public ApiResponse<List<ProductReviewDto>> reviews(@PathVariable UUID productId, HttpServletRequest http) {
        return ApiResponse.ok("Product reviews", productService.reviews(productId), http.getRequestURI());
    }

    /**
     * Adds review.
     *
     * @param productId The product id used to locate the target record.
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/{productId}/reviews")
    public ResponseEntity<ApiResponse<ProductReviewDto>> addReview(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductReviewRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Review added", productService.addReview(productId, request), http.getRequestURI()));
    }

    /**
     * Adds variant.
     *
     * @param productId The product id used to locate the target record.
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/{productId}/variants")
    public ResponseEntity<ApiResponse<ProductVariantDto>> addVariant(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductVariantRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Variant added", productService.addVariant(productId, request), http.getRequestURI()));
    }

    /**
     * Lists variants.
     *
     * @param productId The product id used to locate the target record.
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("/{productId}/variants")
    public ApiResponse<List<ProductVariantDto>> variants(@PathVariable UUID productId, HttpServletRequest http) {
        return ApiResponse.ok("Product variants", productService.listVariants(productId), http.getRequestURI());
    }

    /**
     * Adds media.
     *
     * @param productId The product id used to locate the target record.
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/{productId}/media")
    public ResponseEntity<ApiResponse<ProductMediaDto>> addMedia(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductMediaRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Media added", productService.addMedia(productId, request), http.getRequestURI()));
    }

    /**
     * Executes upsert inventory.
     *
     * @param productId The product id used to locate the target record.
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PutMapping("/{productId}/inventory")
    public ApiResponse<ProductInventoryDto> upsertInventory(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductInventoryRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Inventory upserted", productService.upsertInventory(productId, request), http.getRequestURI());
    }

    /**
     * Executes inventories.
     *
     * @param productId The product id used to locate the target record.
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("/{productId}/inventory")
    public ApiResponse<List<ProductInventoryDto>> inventories(@PathVariable UUID productId, HttpServletRequest http) {
        return ApiResponse.ok("Store inventory", productService.inventories(productId), http.getRequestURI());
    }

    /**
     * Executes related.
     *
     * @param productId The product id used to locate the target record.
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("/{productId}/related")
    public ApiResponse<List<ProductDto>> related(@PathVariable UUID productId, HttpServletRequest http) {
        return ApiResponse.ok("Related products", productService.relatedProducts(productId), http.getRequestURI());
    }

    /**
     * Executes frequently bought together.
     *
     * @param productId The product id used to locate the target record.
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("/{productId}/frequently-bought-together")
    public ApiResponse<List<ProductDto>> frequentlyBoughtTogether(@PathVariable UUID productId, HttpServletRequest http) {
        return ApiResponse.ok("Frequently bought together", productService.frequentlyBoughtTogether(productId), http.getRequestURI());
    }
}
