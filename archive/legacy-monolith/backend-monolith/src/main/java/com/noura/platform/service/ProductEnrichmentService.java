package com.noura.platform.service;

import com.noura.platform.dto.product.ProductEnrichmentResponse;
import com.noura.platform.dto.product.ProductSearchResultDto;

import java.util.List;
import java.util.UUID;

public interface ProductEnrichmentService {

    List<ProductSearchResultDto> searchExistingProducts(String q);

    ProductEnrichmentResponse generateMissingFields(UUID productId);

    ProductEnrichmentResponse generateDescription(UUID productId);

    ProductEnrichmentResponse generateBarcode(UUID productId);

    ProductEnrichmentResponse generateQr(UUID productId);

    byte[] barcodeImage(UUID productId);

    byte[] qrImage(UUID productId);
}
