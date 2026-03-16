package com.noura.platform.search;

import com.noura.platform.dto.product.ProductSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Returns ordered product ids so the service layer can stay stable if search moves to OpenSearch later.
 */
public interface ProductSearchAdapter {
    Page<UUID> searchProductIds(ProductSearchRequest request, Pageable pageable, boolean adminView);
}
