package com.noura.platform.service;

import com.noura.platform.domain.enums.StoreStatus;
import com.noura.platform.domain.enums.StoreType;
import com.noura.platform.dto.location.StoreLocationDto;
import com.noura.platform.dto.location.StoreLocationRequest;
import com.noura.platform.dto.store.CreateStoreRequest;
import com.noura.platform.dto.store.StoreDto;
import com.noura.platform.dto.store.StoreRequest;
import com.noura.platform.dto.store.StoreResponse;
import com.noura.platform.dto.store.UpdateStoreStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Defines store catalog operations for the commerce admin and storefront channels.
 */
public interface StoreService {
    /**
     * Lists stores.
     *
     * @param service The service value.
     * @param openNow The open now value.
     * @param pageable The pagination configuration.
     * @return A paginated result set.
     */
    Page<StoreDto> listStores(String service, Boolean openNow, Pageable pageable);

    /**
     * Creates store.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    StoreDto createStore(StoreRequest request);

    /**
     * Updates store.
     *
     * @param storeId The store id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    StoreDto updateStore(UUID storeId, StoreRequest request);

    /**
     * Deletes store.
     *
     * @param storeId The store id used to locate the target record.
     */
    void deleteStore(UUID storeId);

    /**
     * Finds nearest.
     *
     * @param latitude The latitude value.
     * @param longitude The longitude value.
     * @param limit The limit value.
     * @return A list of matching items.
     */
    List<StoreDto> findNearest(BigDecimal latitude, BigDecimal longitude, int limit);

    /**
     * Sets preferred store.
     *
     * @param storeId The store id used to locate the target record.
     */
    void setPreferredStore(UUID storeId);

    /**
     * Retrieves admin store location settings.
     *
     * @param storeId The store id used to locate the target record.
     * @return The mapped DTO representation.
     */
    StoreLocationDto getStoreLocation(UUID storeId);

    /**
     * Updates admin store location settings.
     *
     * @param storeId The store id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    StoreLocationDto updateStoreLocation(UUID storeId, StoreLocationRequest request);

    /**
     * Lists stores for administration.
     *
     * @param search Optional keyword search across store code, slug, name, and merchant legal/display names.
     * @param merchantId Optional merchant filter.
     * @param type Optional store type filter.
     * @param status Optional store status filter.
     * @param pageable Pagination settings.
     * @return A page of store responses.
     */
    Page<StoreResponse> listAdminStores(
            String search,
            UUID merchantId,
            StoreType type,
            StoreStatus status,
            Pageable pageable
    );

    /**
     * Creates a new merchant-owned store.
     *
     * @param request The request payload.
     * @return The created response.
     */
    StoreResponse createAdminStore(CreateStoreRequest request);

    /**
     * Retrieves a single store for administration.
     *
     * @param storeId The store id used to locate the target record.
     * @return The store response.
     */
    StoreResponse getAdminStore(UUID storeId);

    /**
     * Updates store status in administration workflow.
     *
     * @param storeId The store id used to locate the target record.
     * @param request The status request.
     * @return The updated response.
     */
    StoreResponse updateStoreStatus(UUID storeId, UpdateStoreStatusRequest request);
}
