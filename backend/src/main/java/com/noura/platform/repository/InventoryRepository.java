package com.noura.platform.repository;

import com.noura.platform.domain.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    /**
     * Finds by variant id.
     *
     * @param variantId The variant id used to locate the target record.
     * @return A list of matching items.
     */
    List<Inventory> findByVariantId(UUID variantId);

    /**
     * Finds by variant id and warehouse id.
     *
     * @param variantId The variant id used to locate the target record.
     * @param warehouseId The warehouse id used to locate the target record.
     * @return The result of find by variant id and warehouse id.
     */
    Optional<Inventory> findByVariantIdAndWarehouseId(UUID variantId, UUID warehouseId);
}
