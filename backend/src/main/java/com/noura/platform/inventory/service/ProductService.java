package com.noura.platform.inventory.service;

import com.noura.platform.inventory.dto.product.ProductFilter;
import com.noura.platform.inventory.dto.product.ProductRequest;
import com.noura.platform.inventory.dto.product.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(String productId, ProductRequest request);

    ProductResponse getProduct(String productId);

    Page<ProductResponse> listProducts(ProductFilter filter, Pageable pageable);

    void deleteProduct(String productId);
}
