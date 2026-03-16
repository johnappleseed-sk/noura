package com.noura.platform.service;

import com.noura.platform.common.api.PageResponse;
import com.noura.platform.dto.merchandising.MerchandisingPreviewDto;
import com.noura.platform.dto.merchandising.MerchandisingProductDto;

import java.util.List;
import java.util.UUID;

public interface MerchandisingService {
    PageResponse<MerchandisingProductDto> listProducts(String query, UUID categoryId, UUID storeId, String sort, int page, int size);

    List<MerchandisingProductDto> listProductsForPreview(String query, UUID categoryId, UUID storeId, String sort, int limit);

    MerchandisingPreviewDto preview(String query, UUID categoryId, UUID storeId, int limit);
}
