package com.noura.platform.service;

import com.noura.platform.dto.product.ProductGeneratorRequest;
import com.noura.platform.dto.product.ProductGeneratorResponse;

public interface ProductGeneratorService {

    ProductGeneratorResponse generate(ProductGeneratorRequest request);
}
