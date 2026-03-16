package com.noura.inventory.repository;

import com.noura.inventory.domain.entity.InventoryStockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Persistence gateway for stock movement history.
 */
public interface InventoryStockMovementRepository extends JpaRepository<InventoryStockMovement, UUID> {

    /**
     * Returns the latest movement records for a product.
     *
     * @param productId product identifier
     * @return up to 50 latest product movements
     */
    List<InventoryStockMovement> findTop50ByProductIdOrderByCreatedAtDesc(UUID productId);

    /**
     * Returns the latest movement records for a product at a specific warehouse.
     *
     * @param productId product identifier
     * @param warehouseId warehouse/location identifier
     * @return up to 50 latest product/location movements
     */
    List<InventoryStockMovement> findTop50ByProductIdAndWarehouseIdOrderByCreatedAtDesc(UUID productId, UUID warehouseId);
}
