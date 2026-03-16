package com.noura.inventory.service.impl;

import com.noura.inventory.domain.entity.InventoryStockLevel;
import com.noura.inventory.domain.entity.InventoryStockMovement;
import com.noura.inventory.domain.enums.StockMovementType;
import com.noura.inventory.domain.enums.StockStatus;
import com.noura.inventory.dto.stock.AdjustmentMovementRequest;
import com.noura.inventory.dto.stock.StockAdjustmentRequest;
import com.noura.inventory.dto.stock.StockDeductionRequest;
import com.noura.inventory.dto.stock.StockLevelResponse;
import com.noura.inventory.dto.stock.StockOperationResponse;
import com.noura.inventory.dto.stock.StockReleaseReservationRequest;
import com.noura.inventory.dto.stock.StockReservationRequest;
import com.noura.inventory.exception.InventoryOperationException;
import com.noura.inventory.exception.NotFoundException;
import com.noura.inventory.repository.InventoryStockLevelRepository;
import com.noura.inventory.repository.InventoryStockMovementRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Default implementation of {@link com.noura.inventory.service.InventoryStockService}.
 *
 * <p>All write flows run in transactions and use pessimistic row locking to avoid
 * lost updates under concurrent stock mutations.</p>
 */
@Service
@RequiredArgsConstructor
public class InventoryStockServiceImpl implements com.noura.inventory.service.InventoryStockService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    private static final BigDecimal DEFAULT_LOW_STOCK_THRESHOLD = BigDecimal.valueOf(5).setScale(4, RoundingMode.HALF_UP);

    private final InventoryStockLevelRepository stockLevelRepository;
    private final InventoryStockMovementRepository movementRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<StockLevelResponse> listStockLevels(
            UUID productId,
            UUID warehouseId,
            UUID binId,
            UUID batchId,
            Boolean lowStockOnly,
            Pageable pageable
    ) {
        Specification<InventoryStockLevel> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (productId != null) {
                predicates.add(cb.equal(root.get("productId"), productId));
            }
            if (warehouseId != null) {
                predicates.add(cb.equal(root.get("warehouseId"), warehouseId));
            }
            if (binId != null) {
                predicates.add(cb.equal(root.get("binId"), binId));
            }
            if (batchId != null) {
                predicates.add(cb.equal(root.get("batchId"), batchId));
            }
            if (Boolean.TRUE.equals(lowStockOnly)) {
                // Low-stock includes both low and fully out-of-stock items.
                predicates.add(root.get("stockStatus").in(StockStatus.LOW_STOCK, StockStatus.OUT_OF_STOCK));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return stockLevelRepository.findAll(specification, pageable).map(this::toResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<StockLevelResponse> getByProduct(UUID productId) {
        return stockLevelRepository.findByProductIdOrderByUpdatedAtDesc(productId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public StockLevelResponse getByProductAndLocation(UUID productId, UUID locationId) {
        InventoryStockLevel stockLevel = stockLevelRepository.findByProductIdAndWarehouseId(productId, locationId)
                .orElseThrow(() -> new NotFoundException("STOCK_LEVEL_NOT_FOUND", "Stock level not found for product/location"));
        return toResponse(stockLevel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<StockLevelResponse> getLowStock(Pageable pageable) {
        return stockLevelRepository.findByStockStatusInOrderByQuantityAvailableAsc(
                        List.of(StockStatus.LOW_STOCK, StockStatus.OUT_OF_STOCK),
                        pageable
                )
                .map(this::toResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public StockOperationResponse adjust(StockAdjustmentRequest request, String actorUserId) {
        if (normalize(request.quantityDelta()).signum() == 0) {
            throw new InventoryOperationException(HttpStatus.BAD_REQUEST, "INVALID_ADJUSTMENT", "quantityDelta cannot be zero");
        }
        InventoryStockLevel stockLevel = getOrCreateStockLevelForUpdate(request.productId(), request.locationId(), normalizeActor(actorUserId));
        if (request.lowStockThreshold() != null) {
            BigDecimal threshold = normalizeNonNegative(request.lowStockThreshold(), "lowStockThreshold");
            stockLevel.setLowStockThreshold(threshold);
        }
        return applyAdjustment(stockLevel, normalize(request.quantityDelta()), request.reasonCode(),
                request.referenceType(), request.referenceId(), request.notes(), normalizeActor(actorUserId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public List<StockOperationResponse> adjustCompatibility(AdjustmentMovementRequest request, String actorUserId) {
        List<StockOperationResponse> responses = new ArrayList<>();
        String actor = normalizeActor(actorUserId);
        for (var line : request.lines()) {
            InventoryStockLevel stockLevel = getOrCreateStockLevelForUpdate(line.productId(), request.warehouseId(), actor);
            stockLevel.setBinId(request.binId());
            stockLevel.setBatchId(line.batchId());
            stockLevel.setLotNumber(line.lotNumber());
            responses.add(applyAdjustment(
                    stockLevel,
                    normalize(line.quantityDelta()),
                    request.reasonCode(),
                    request.referenceType(),
                    request.referenceId(),
                    coalesce(line.notes(), request.notes()),
                    actor
            ));
        }
        return responses;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public StockOperationResponse reserve(StockReservationRequest request, String actorUserId) {
        String actor = normalizeActor(actorUserId);
        InventoryStockLevel stockLevel = getRequiredStockLevelForUpdate(request.productId(), request.locationId());
        BigDecimal quantity = normalizePositive(request.quantity(), "quantity");

        BigDecimal onHandBefore = normalize(stockLevel.getQuantityOnHand());
        BigDecimal reservedBefore = normalize(stockLevel.getQuantityReserved());
        BigDecimal availableBefore = normalize(stockLevel.getQuantityAvailable());

        if (availableBefore.compareTo(quantity) < 0) {
            throw new InventoryOperationException(
                    HttpStatus.CONFLICT,
                    "INSUFFICIENT_STOCK",
                    "Available quantity is lower than requested reservation"
            );
        }

        stockLevel.setQuantityReserved(reservedBefore.add(quantity));
        stockLevel.setLastMovementAt(Instant.now());
        stockLevel.setUpdatedBy(actor);
        stockLevel.recalculateAvailability();
        stockLevelRepository.save(stockLevel);

        InventoryStockMovement movement = createMovement(
                stockLevel,
                StockMovementType.RESERVE,
                quantity,
                onHandBefore,
                reservedBefore,
                availableBefore,
                request.reasonCode(),
                request.referenceType(),
                request.referenceId(),
                request.notes(),
                actor
        );
        return new StockOperationResponse(movement.getId(), movement.getMovementType(), toResponse(stockLevel));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public StockOperationResponse releaseReservation(StockReleaseReservationRequest request, String actorUserId) {
        String actor = normalizeActor(actorUserId);
        InventoryStockLevel stockLevel = getRequiredStockLevelForUpdate(request.productId(), request.locationId());
        BigDecimal quantity = normalizePositive(request.quantity(), "quantity");

        BigDecimal onHandBefore = normalize(stockLevel.getQuantityOnHand());
        BigDecimal reservedBefore = normalize(stockLevel.getQuantityReserved());
        BigDecimal availableBefore = normalize(stockLevel.getQuantityAvailable());

        if (reservedBefore.compareTo(quantity) < 0) {
            throw new InventoryOperationException(
                    HttpStatus.CONFLICT,
                    "INSUFFICIENT_RESERVED_STOCK",
                    "Reserved quantity is lower than requested release amount"
            );
        }

        stockLevel.setQuantityReserved(reservedBefore.subtract(quantity));
        stockLevel.setLastMovementAt(Instant.now());
        stockLevel.setUpdatedBy(actor);
        stockLevel.recalculateAvailability();
        stockLevelRepository.save(stockLevel);

        InventoryStockMovement movement = createMovement(
                stockLevel,
                StockMovementType.RELEASE,
                quantity.negate(),
                onHandBefore,
                reservedBefore,
                availableBefore,
                request.reasonCode(),
                request.referenceType(),
                request.referenceId(),
                request.notes(),
                actor
        );
        return new StockOperationResponse(movement.getId(), movement.getMovementType(), toResponse(stockLevel));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public StockOperationResponse deduct(StockDeductionRequest request, String actorUserId) {
        String actor = normalizeActor(actorUserId);
        InventoryStockLevel stockLevel = getRequiredStockLevelForUpdate(request.productId(), request.locationId());
        BigDecimal quantity = normalizePositive(request.quantity(), "quantity");
        boolean consumeReserved = request.consumeReserved() == null || request.consumeReserved();

        BigDecimal onHandBefore = normalize(stockLevel.getQuantityOnHand());
        BigDecimal reservedBefore = normalize(stockLevel.getQuantityReserved());
        BigDecimal availableBefore = normalize(stockLevel.getQuantityAvailable());

        if (consumeReserved) {
            // Checkout/fulfillment path: consume from reserved bucket first.
            if (reservedBefore.compareTo(quantity) < 0) {
                throw new InventoryOperationException(
                        HttpStatus.CONFLICT,
                        "INSUFFICIENT_RESERVED_STOCK",
                        "Reserved quantity is lower than deduction request"
                );
            }
            stockLevel.setQuantityReserved(reservedBefore.subtract(quantity));
            stockLevel.setQuantityOnHand(onHandBefore.subtract(quantity));
        } else {
            // Admin/manual path: deduct directly from currently available stock.
            if (availableBefore.compareTo(quantity) < 0) {
                throw new InventoryOperationException(
                        HttpStatus.CONFLICT,
                        "INSUFFICIENT_STOCK",
                        "Available quantity is lower than deduction request"
                );
            }
            stockLevel.setQuantityOnHand(onHandBefore.subtract(quantity));
        }

        if (normalize(stockLevel.getQuantityOnHand()).signum() < 0) {
            throw new InventoryOperationException(
                    HttpStatus.CONFLICT,
                    "NEGATIVE_ON_HAND_NOT_ALLOWED",
                    "Deduction cannot make on-hand quantity negative"
            );
        }

        stockLevel.setLastMovementAt(Instant.now());
        stockLevel.setUpdatedBy(actor);
        stockLevel.recalculateAvailability();
        stockLevelRepository.save(stockLevel);

        InventoryStockMovement movement = createMovement(
                stockLevel,
                StockMovementType.DEDUCT,
                quantity.negate(),
                onHandBefore,
                reservedBefore,
                availableBefore,
                request.reasonCode(),
                request.referenceType(),
                request.referenceId(),
                request.notes(),
                actor
        );
        return new StockOperationResponse(movement.getId(), movement.getMovementType(), toResponse(stockLevel));
    }

    /**
     * Applies a normalized quantity delta and records a movement entry.
     *
     * @param stockLevel mutable stock level
     * @param quantityDelta signed delta
     * @param reasonCode optional reason code
     * @param referenceType optional reference type
     * @param referenceId optional reference id
     * @param notes optional note
     * @param actor acting principal
     * @return operation response
     * @throws InventoryOperationException when constraints are violated
     */
    private StockOperationResponse applyAdjustment(
            InventoryStockLevel stockLevel,
            BigDecimal quantityDelta,
            String reasonCode,
            String referenceType,
            String referenceId,
            String notes,
            String actor
    ) {
        BigDecimal onHandBefore = normalize(stockLevel.getQuantityOnHand());
        BigDecimal reservedBefore = normalize(stockLevel.getQuantityReserved());
        BigDecimal availableBefore = normalize(stockLevel.getQuantityAvailable());

        BigDecimal onHandAfter = onHandBefore.add(quantityDelta);
        if (onHandAfter.signum() < 0) {
            throw new InventoryOperationException(
                    HttpStatus.CONFLICT,
                    "NEGATIVE_ON_HAND_NOT_ALLOWED",
                    "Adjustment cannot make on-hand quantity negative"
            );
        }
        if (onHandAfter.compareTo(reservedBefore) < 0) {
            throw new InventoryOperationException(
                    HttpStatus.CONFLICT,
                    "ON_HAND_BELOW_RESERVED",
                    "Adjustment cannot reduce on-hand below reserved quantity"
            );
        }

        stockLevel.setQuantityOnHand(onHandAfter);
        stockLevel.setLastMovementAt(Instant.now());
        stockLevel.setUpdatedBy(actor);
        stockLevel.recalculateAvailability();
        stockLevelRepository.save(stockLevel);

        InventoryStockMovement movement = createMovement(
                stockLevel,
                StockMovementType.ADJUSTMENT,
                quantityDelta,
                onHandBefore,
                reservedBefore,
                availableBefore,
                reasonCode,
                referenceType,
                referenceId,
                notes,
                actor
        );
        return new StockOperationResponse(movement.getId(), movement.getMovementType(), toResponse(stockLevel));
    }

    /**
     * Retrieves a stock level with write lock, failing when absent.
     *
     * @param productId product identifier
     * @param warehouseId warehouse/location identifier
     * @return locked stock level
     * @throws NotFoundException when stock level does not exist
     */
    private InventoryStockLevel getRequiredStockLevelForUpdate(UUID productId, UUID warehouseId) {
        return stockLevelRepository.findByProductIdAndWarehouseIdForUpdate(productId, warehouseId)
                .orElseThrow(() -> new NotFoundException(
                        "STOCK_LEVEL_NOT_FOUND",
                        "Stock level not found for product/location"
                ));
    }

    /**
     * Retrieves a stock level with write lock or creates one if absent.
     *
     * @param productId product identifier
     * @param warehouseId warehouse/location identifier
     * @param actor acting principal
     * @return existing or newly created stock level
     */
    private InventoryStockLevel getOrCreateStockLevelForUpdate(UUID productId, UUID warehouseId, String actor) {
        return stockLevelRepository.findByProductIdAndWarehouseIdForUpdate(productId, warehouseId)
                .orElseGet(() -> createStockLevel(productId, warehouseId, actor));
    }

    /**
     * Creates a baseline stock level record.
     *
     * <p>If a concurrent request inserts the same product/location pair, this method
     * reloads the existing row after unique-key conflict.</p>
     *
     * @param productId product identifier
     * @param warehouseId warehouse/location identifier
     * @param actor acting principal
     * @return persisted stock level
     */
    private InventoryStockLevel createStockLevel(UUID productId, UUID warehouseId, String actor) {
        InventoryStockLevel created = new InventoryStockLevel();
        created.setProductId(productId);
        created.setWarehouseId(warehouseId);
        created.setProductSku(productId.toString());
        created.setProductName("Product " + shortId(productId));
        created.setWarehouseCode(warehouseId.toString());
        created.setWarehouseName("Warehouse " + shortId(warehouseId));
        created.setQuantityOnHand(ZERO);
        created.setQuantityReserved(ZERO);
        created.setQuantityAvailable(ZERO);
        created.setQuantityDamaged(ZERO);
        created.setLowStockThreshold(DEFAULT_LOW_STOCK_THRESHOLD);
        created.setCreatedBy(actor);
        created.setUpdatedBy(actor);
        created.recalculateAvailability();
        try {
            return stockLevelRepository.save(created);
        } catch (DataIntegrityViolationException duplicateInsert) {
            // Handles race: another transaction may have created the row first.
            return stockLevelRepository.findByProductIdAndWarehouseIdForUpdate(productId, warehouseId)
                    .orElseThrow(() -> duplicateInsert);
        }
    }

    /**
     * Persists a stock movement audit record.
     *
     * @param stockLevel current stock level state
     * @param movementType movement type
     * @param quantityDelta signed quantity delta
     * @param onHandBefore on-hand before mutation
     * @param reservedBefore reserved before mutation
     * @param availableBefore available before mutation
     * @param reasonCode optional reason code
     * @param referenceType optional reference type
     * @param referenceId optional reference id
     * @param notes optional note
     * @param actor acting principal
     * @return persisted movement record
     */
    private InventoryStockMovement createMovement(
            InventoryStockLevel stockLevel,
            StockMovementType movementType,
            BigDecimal quantityDelta,
            BigDecimal onHandBefore,
            BigDecimal reservedBefore,
            BigDecimal availableBefore,
            String reasonCode,
            String referenceType,
            String referenceId,
            String notes,
            String actor
    ) {
        InventoryStockMovement movement = new InventoryStockMovement();
        movement.setStockLevelId(stockLevel.getId());
        movement.setProductId(stockLevel.getProductId());
        movement.setWarehouseId(stockLevel.getWarehouseId());
        movement.setMovementType(movementType);
        movement.setQuantityDelta(normalize(quantityDelta));
        movement.setQuantityOnHandBefore(normalize(onHandBefore));
        movement.setQuantityOnHandAfter(normalize(stockLevel.getQuantityOnHand()));
        movement.setQuantityReservedBefore(normalize(reservedBefore));
        movement.setQuantityReservedAfter(normalize(stockLevel.getQuantityReserved()));
        movement.setQuantityAvailableBefore(normalize(availableBefore));
        movement.setQuantityAvailableAfter(normalize(stockLevel.getQuantityAvailable()));
        movement.setReasonCode(trimToNull(reasonCode));
        movement.setReferenceType(trimToNull(referenceType));
        movement.setReferenceId(trimToNull(referenceId));
        movement.setNotes(trimToNull(notes));
        movement.setCreatedBy(actor);
        movement.setUpdatedBy(actor);
        return movementRepository.save(movement);
    }

    /**
     * Maps entity state into API response model.
     *
     * @param stockLevel stock level entity
     * @return response DTO
     */
    private StockLevelResponse toResponse(InventoryStockLevel stockLevel) {
        boolean lowStock = stockLevel.getStockStatus() == StockStatus.LOW_STOCK
                || stockLevel.getStockStatus() == StockStatus.OUT_OF_STOCK;
        return new StockLevelResponse(
                stockLevel.getId(),
                stockLevel.getProductId(),
                coalesce(stockLevel.getProductSku(), stockLevel.getProductId().toString()),
                coalesce(stockLevel.getProductName(), "Product " + shortId(stockLevel.getProductId())),
                stockLevel.getWarehouseId(),
                coalesce(stockLevel.getWarehouseCode(), stockLevel.getWarehouseId().toString()),
                coalesce(stockLevel.getWarehouseName(), "Warehouse " + shortId(stockLevel.getWarehouseId())),
                stockLevel.getBinId(),
                stockLevel.getBinCode(),
                stockLevel.getBatchId(),
                stockLevel.getLotNumber(),
                normalize(stockLevel.getQuantityOnHand()),
                normalize(stockLevel.getQuantityReserved()),
                normalize(stockLevel.getQuantityAvailable()),
                normalize(stockLevel.getQuantityDamaged()),
                stockLevel.getLastMovementAt(),
                lowStock,
                normalizeNonNegative(stockLevel.getLowStockThreshold(), "lowStockThreshold"),
                stockLevel.getStockStatus(),
                stockLevel.getUpdatedAt()
        );
    }

    /**
     * Normalizes decimal values to scale 4.
     *
     * @param value raw value
     * @return normalized value, or zero when null
     */
    private BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Normalizes and validates a strictly positive decimal.
     *
     * @param value raw value
     * @param fieldName field label for validation messaging
     * @return normalized positive value
     * @throws InventoryOperationException when value is zero or negative
     */
    private BigDecimal normalizePositive(BigDecimal value, String fieldName) {
        BigDecimal normalized = normalize(value);
        if (normalized.signum() <= 0) {
            throw new InventoryOperationException(HttpStatus.BAD_REQUEST, "INVALID_" + fieldName.toUpperCase(Locale.ROOT),
                    fieldName + " must be greater than zero");
        }
        return normalized;
    }

    /**
     * Normalizes and validates a non-negative decimal.
     *
     * @param value raw value
     * @param fieldName field label for validation messaging
     * @return normalized non-negative value
     * @throws InventoryOperationException when value is negative
     */
    private BigDecimal normalizeNonNegative(BigDecimal value, String fieldName) {
        BigDecimal normalized = normalize(value);
        if (normalized.signum() < 0) {
            throw new InventoryOperationException(HttpStatus.BAD_REQUEST, "INVALID_" + fieldName.toUpperCase(Locale.ROOT),
                    fieldName + " must be non-negative");
        }
        return normalized;
    }

    /**
     * Normalizes actor identity for audit fields.
     *
     * @param actor raw actor value
     * @return trimmed actor or {@code system} fallback
     */
    private String normalizeActor(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    /**
     * Converts blank strings to null.
     *
     * @param value raw value
     * @return trimmed value or null
     */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * Builds short display token from UUID.
     *
     * @param value UUID value
     * @return first eight characters of UUID string
     */
    private String shortId(UUID value) {
        return value.toString().substring(0, 8);
    }

    /**
     * Returns first non-blank value.
     *
     * @param value preferred value
     * @param fallback fallback value
     * @return preferred value if non-blank, otherwise fallback
     */
    private String coalesce(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
