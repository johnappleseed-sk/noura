package com.noura.platform.service;

import com.noura.platform.dto.product.ProductSearchRequest;
import com.noura.platform.dto.product.ProductSearchResponse;
import org.springframework.data.domain.Page;

public interface ProductSearchService {
    Page<ProductSearchResponse> searchPublic(ProductSearchRequest request);

    Page<ProductSearchResponse> searchAdmin(ProductSearchRequest request);
}
