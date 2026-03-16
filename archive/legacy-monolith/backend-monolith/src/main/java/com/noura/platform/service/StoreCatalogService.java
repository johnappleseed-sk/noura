package com.noura.platform.service;

import com.noura.platform.dto.product.ProductDto;
import com.noura.platform.dto.product.ProductInventoryDto;
import com.noura.platform.dto.product.StoreProductAdoptionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Store catalog adoption layer that links master products into a store with local overrides.
 */
public interface StoreCatalogService {
    Page<ProductDto> searchAdoptableMasterProducts(UUID storeId, String query, Pageable pageable);

    ProductInventoryDto adoptMasterProduct(UUID storeId, UUID masterProductId, StoreProductAdoptionRequest request);

    ProductInventoryDto updateStoreProduct(UUID storeId, UUID masterProductId, StoreProductAdoptionRequest request);
}

