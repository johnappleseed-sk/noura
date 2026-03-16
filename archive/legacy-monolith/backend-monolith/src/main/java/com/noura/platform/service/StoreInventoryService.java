package com.noura.platform.service;

import com.noura.platform.dto.inventory.InventoryAdjustRequest;
import com.noura.platform.dto.inventory.InventoryLevelDto;
import com.noura.platform.dto.inventory.InventorySummaryDto;

import java.util.UUID;

/**
 * Store-scoped inventory operations over warehouses owned by a store tenant.
 */
public interface StoreInventoryService {
    InventorySummaryDto stock(UUID storeId, UUID variantId);

    InventoryLevelDto adjust(UUID storeId, InventoryAdjustRequest request);
}

