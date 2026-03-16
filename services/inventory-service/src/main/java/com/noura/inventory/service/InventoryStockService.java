package com.noura.inventory.service;

import com.noura.inventory.dto.stock.AdjustmentMovementRequest;
import com.noura.inventory.dto.stock.StockAdjustmentRequest;
import com.noura.inventory.dto.stock.StockDeductionRequest;
import com.noura.inventory.dto.stock.StockLevelResponse;
import com.noura.inventory.dto.stock.StockOperationResponse;
import com.noura.inventory.dto.stock.StockReleaseReservationRequest;
import com.noura.inventory.dto.stock.StockReservationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Application service for stock query and mutation use cases.
 */
public interface InventoryStockService {

    /**
     * Returns paged stock levels using optional filters.
     *
     * @param productId optional product filter
     * @param warehouseId optional warehouse/location filter
     * @param binId optional bin filter
     * @param batchId optional batch filter
     * @param lowStockOnly optional low-stock filter
     * @param pageable page/sort request
     * @return stock levels page
     */
    Page<StockLevelResponse> listStockLevels(
            UUID productId,
            UUID warehouseId,
            UUID binId,
            UUID batchId,
            Boolean lowStockOnly,
            Pageable pageable
    );

    /**
     * Returns all stock levels for a product.
     *
     * @param productId product identifier
     * @return stock levels for the product
     */
    List<StockLevelResponse> getByProduct(UUID productId);

    /**
     * Returns stock level for a product/location pair.
     *
     * @param productId product identifier
     * @param locationId warehouse/location identifier
     * @return stock level
     * @throws com.noura.inventory.exception.NotFoundException when stock level does not exist
     */
    StockLevelResponse getByProductAndLocation(UUID productId, UUID locationId);

    /**
     * Returns low-stock entries ordered by available quantity.
     *
     * @param pageable page request
     * @return low-stock page
     */
    Page<StockLevelResponse> getLowStock(Pageable pageable);

    /**
     * Applies a stock adjustment for one product/location.
     *
     * @param request adjustment request
     * @param actorUserId optional actor identity
     * @return operation result with resulting stock state
     * @throws com.noura.inventory.exception.InventoryOperationException when business rules are violated
     */
    StockOperationResponse adjust(StockAdjustmentRequest request, String actorUserId);

    /**
     * Applies legacy-style bulk adjustments for compatibility.
     *
     * @param request adjustment request with multiple lines
     * @param actorUserId optional actor identity
     * @return operation results for each line
     * @throws com.noura.inventory.exception.InventoryOperationException when business rules are violated
     */
    List<StockOperationResponse> adjustCompatibility(AdjustmentMovementRequest request, String actorUserId);

    /**
     * Reserves available stock.
     *
     * @param request reservation request
     * @param actorUserId optional actor identity
     * @return operation result with resulting stock state
     * @throws com.noura.inventory.exception.InventoryOperationException when stock is insufficient
     */
    StockOperationResponse reserve(StockReservationRequest request, String actorUserId);

    /**
     * Releases previously reserved stock.
     *
     * @param request release request
     * @param actorUserId optional actor identity
     * @return operation result with resulting stock state
     * @throws com.noura.inventory.exception.InventoryOperationException when reserved stock is insufficient
     */
    StockOperationResponse releaseReservation(StockReleaseReservationRequest request, String actorUserId);

    /**
     * Deducts stock (reservation consumption or direct available deduction).
     *
     * @param request deduction request
     * @param actorUserId optional actor identity
     * @return operation result with resulting stock state
     * @throws com.noura.inventory.exception.InventoryOperationException when stock constraints fail
     */
    StockOperationResponse deduct(StockDeductionRequest request, String actorUserId);
}
