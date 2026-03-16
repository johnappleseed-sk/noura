package com.noura.inventory.repository;

import com.noura.inventory.domain.entity.InventoryStockLevel;
import com.noura.inventory.domain.enums.StockStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence gateway for stock level state.
 */
public interface InventoryStockLevelRepository extends JpaRepository<InventoryStockLevel, UUID>, JpaSpecificationExecutor<InventoryStockLevel> {

    /**
     * Returns stock levels for a product ordered by most recently updated first.
     *
     * @param productId product identifier
     * @return stock levels for the product
     */
    List<InventoryStockLevel> findByProductIdOrderByUpdatedAtDesc(UUID productId);

    /**
     * Returns stock level by product and warehouse without locking.
     *
     * @param productId product identifier
     * @param warehouseId warehouse/location identifier
     * @return stock level if present
     */
    Optional<InventoryStockLevel> findByProductIdAndWarehouseId(UUID productId, UUID warehouseId);

    /**
     * Returns stock level by product and warehouse using pessimistic write locking.
     *
     * <p>Used for mutation flows to avoid lost updates under concurrent writes.</p>
     *
     * @param productId product identifier
     * @param warehouseId warehouse/location identifier
     * @return locked stock level if present
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select sl from InventoryStockLevel sl
            where sl.productId = :productId
              and sl.warehouseId = :warehouseId
            """)
    Optional<InventoryStockLevel> findByProductIdAndWarehouseIdForUpdate(
            @Param("productId") UUID productId,
            @Param("warehouseId") UUID warehouseId
    );

    /**
     * Returns low-stock pages for the supplied statuses ordered by availability ascending.
     *
     * @param statuses statuses included in low-stock view
     * @param pageable page request
     * @return low-stock page
     */
    Page<InventoryStockLevel> findByStockStatusInOrderByQuantityAvailableAsc(
            Collection<StockStatus> statuses,
            Pageable pageable
    );
}
