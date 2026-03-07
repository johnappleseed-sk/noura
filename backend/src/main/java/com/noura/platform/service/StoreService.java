package com.noura.platform.service;

import com.noura.platform.dto.store.StoreDto;
import com.noura.platform.dto.store.StoreRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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
}
