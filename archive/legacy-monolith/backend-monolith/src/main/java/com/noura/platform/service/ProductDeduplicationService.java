package com.noura.platform.service;

import com.noura.platform.domain.entity.Product;
import com.noura.platform.domain.entity.ProductSubmission;

import java.util.Optional;

public interface ProductDeduplicationService {
    String buildSimilarityHash(String proposedName, String proposedBarcode, String proposedBrand);

    Optional<Product> findPotentialMatch(ProductSubmission submission);
}
