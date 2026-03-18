package com.noura.shipping.service;

import com.noura.shipping.common.PageResponse;
import com.noura.shipping.dto.network.MerchantCreateRequest;
import com.noura.shipping.dto.network.MerchantResponse;
import com.noura.shipping.dto.network.MerchantStatusUpdateRequest;
import com.noura.shipping.dto.network.ServiceAreaRequest;
import com.noura.shipping.dto.network.ServiceAreaResponse;
import com.noura.shipping.dto.network.ServiceAreaValidationRequest;
import com.noura.shipping.dto.network.ServiceEligibilityResponse;
import com.noura.shipping.dto.network.StoreLocationResponse;
import com.noura.shipping.dto.network.StoreLocationUpdateRequest;
import com.noura.shipping.dto.network.StoreRequest;
import com.noura.shipping.dto.network.StoreResponse;
import com.noura.shipping.dto.network.StoreStatusUpdateRequest;
import com.noura.shipping.domain.enums.MerchantStatus;
import com.noura.shipping.domain.enums.ServiceAreaStatus;
import com.noura.shipping.domain.enums.ServiceAreaType;
import com.noura.shipping.domain.enums.StoreStatus;
import com.noura.shipping.domain.enums.StoreType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Compatibility service for admin-web merchant, store, and service-area flows.
 */
public interface FulfillmentNetworkService {

    PageResponse<MerchantResponse> listMerchants(String search, MerchantStatus status, int page, int size, String sortBy, String direction);

    MerchantResponse getMerchant(UUID merchantId);

    MerchantResponse createMerchant(MerchantCreateRequest request, String actorUserId);

    MerchantResponse updateMerchantStatus(UUID merchantId, MerchantStatusUpdateRequest request, String actorUserId);

    PageResponse<StoreResponse> listAdminStores(
            String search,
            UUID merchantId,
            StoreType type,
            StoreStatus status,
            int page,
            int size,
            String sortBy,
            String direction
    );

    StoreResponse getAdminStore(UUID storeId);

    StoreResponse createAdminStore(StoreRequest request, String actorUserId);

    StoreResponse updateStoreStatus(UUID storeId, StoreStatusUpdateRequest request, String actorUserId);

    PageResponse<StoreResponse> listStores(String service, Boolean openNow, int page, int size, String sortBy, String direction);

    StoreResponse createStore(StoreRequest request, String actorUserId);

    StoreResponse updateStore(UUID storeId, StoreRequest request, String actorUserId);

    void deleteStore(UUID storeId, String actorUserId);

    StoreLocationResponse getStoreLocation(UUID storeId);

    StoreLocationResponse updateStoreLocation(UUID storeId, StoreLocationUpdateRequest request, String actorUserId);

    void setPreferredStore(UUID storeId, String actorUserId);

    java.util.List<StoreResponse> findNearestStores(BigDecimal latitude, BigDecimal longitude, int limit);

    PageResponse<ServiceAreaResponse> listServiceAreas(
            String query,
            ServiceAreaStatus status,
            ServiceAreaType type,
            int page,
            int size,
            String sortBy,
            String direction
    );

    ServiceAreaResponse getServiceArea(UUID serviceAreaId);

    ServiceAreaResponse createServiceArea(ServiceAreaRequest request, String actorUserId);

    ServiceAreaResponse updateServiceArea(UUID serviceAreaId, ServiceAreaRequest request, String actorUserId);

    void deleteServiceArea(UUID serviceAreaId, String actorUserId);

    ServiceAreaResponse activateServiceArea(UUID serviceAreaId, String actorUserId);

    ServiceAreaResponse deactivateServiceArea(UUID serviceAreaId, String actorUserId);

    ServiceEligibilityResponse validateServiceArea(ServiceAreaValidationRequest request);
}
