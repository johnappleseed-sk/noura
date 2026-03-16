package com.noura.platform.service;

import com.noura.platform.domain.enums.MerchantStatus;
import com.noura.platform.dto.merchant.CreateMerchantRequest;
import com.noura.platform.dto.merchant.MerchantResponse;
import com.noura.platform.dto.merchant.UpdateMerchantStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MerchantService {
    Page<MerchantResponse> listMerchants(String search, MerchantStatus status, Pageable pageable);

    MerchantResponse createMerchant(CreateMerchantRequest request);

    MerchantResponse getMerchant(UUID merchantId);

    MerchantResponse updateMerchantStatus(UUID merchantId, UpdateMerchantStatusRequest request);
}
