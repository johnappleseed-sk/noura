package com.noura.platform.service;

import com.noura.platform.dto.brand.BrandResponse;
import com.noura.platform.dto.brand.CreateBrandRequest;

import java.util.List;

public interface BrandService {
    BrandResponse createBrand(CreateBrandRequest request);

    List<BrandResponse> listBrands();
}
