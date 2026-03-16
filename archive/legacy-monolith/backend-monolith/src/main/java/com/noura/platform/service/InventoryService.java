package com.noura.platform.service;

import com.noura.platform.dto.inventory.*;

import java.util.UUID;

public interface InventoryService {
    /**
     * Creates warehouse.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    WarehouseDto createWarehouse(WarehouseRequest request);

    /**
     * Retrieves warehouses.
     *
     * @return A list of matching items.
     */
    java.util.List<WarehouseDto> warehouses();

    /**
     * Retrieves stock.
     *
     * @param variantId The variant id used to locate the target record.
     * @return The mapped DTO representation.
     */
    InventorySummaryDto stock(UUID variantId);

    /**
     * Adjusts stock.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    InventoryLevelDto adjust(InventoryAdjustRequest request);

    /**
     * Reserves stock.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    InventoryReservationDto reserve(InventoryReserveRequest request);

    /**
     * Confirms reservation.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    InventoryReservationDto confirm(InventoryReservationActionRequest request);

    /**
     * Releases reservation.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    InventoryReservationDto release(InventoryReservationActionRequest request);

    /**
     * Checks availability.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    InventoryCheckResultDto check(InventoryCheckRequest request);
}
