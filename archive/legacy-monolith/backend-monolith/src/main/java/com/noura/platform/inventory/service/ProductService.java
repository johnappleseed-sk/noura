package com.noura.platform.inventory.service;

import com.noura.platform.inventory.dto.product.ProductFilter;
import com.noura.platform.inventory.dto.product.ProductRequest;
import com.noura.platform.inventory.dto.product.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Defines governed inventory product management operations.
 */
public interface ProductService {

    /**
     * Creates a new inventory product.
     *
     * @param request The product request payload.
     * @return The created product response.
     */
    ProductResponse createProduct(ProductRequest request);

    /**
     * Updates an existing inventory product.
     *
     * @param productId The inventory product identifier.
     * @param request The product request payload.
     * @return The updated product response.
     */
    ProductResponse updateProduct(String productId, ProductRequest request);

    /**
     * Retrieves a single inventory product.
     *
     * @param productId The inventory product identifier.
     * @return The resolved product response.
     */
    ProductResponse getProduct(String productId);

    /**
     * Lists inventory products for the supplied filter.
     *
     * @param filter The product filter.
     * @param pageable The pagination configuration.
     * @return The paginated product response.
     */
    Page<ProductResponse> listProducts(ProductFilter filter, Pageable pageable);

    /**
     * Moves an inventory product into governed trash.
     *
     * @param productId The inventory product identifier.
     */
    void deleteProduct(String productId);
}
