package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.*;
import com.noura.platform.domain.enums.InventoryReservationStatus;
import com.noura.platform.domain.enums.InventoryTransactionType;
import com.noura.platform.dto.inventory.*;
import com.noura.platform.repository.*;
import com.noura.platform.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ProductVariantRepository productVariantRepository;
    private final WarehouseRepository warehouseRepository;

    /**
     * Creates warehouse.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public WarehouseDto createWarehouse(WarehouseRequest request) {
        warehouseRepository.findByNameIgnoreCase(request.name()).ifPresent(existing -> {
            throw new BadRequestException("WAREHOUSE_EXISTS", "Warehouse name already exists");
        });
        Warehouse warehouse = new Warehouse();
        warehouse.setName(request.name().trim());
        warehouse.setLocation(request.location().trim());
        warehouse.setActive(request.active() == null || request.active());
        Warehouse saved = warehouseRepository.save(warehouse);
        return new WarehouseDto(saved.getId(), saved.getName(), saved.getLocation(), saved.isActive());
    }

    /**
     * Retrieves warehouses.
     *
     * @return A list of matching items.
     */
    @Override
    public List<WarehouseDto> warehouses() {
        return warehouseRepository.findAll().stream()
                .map(item -> new WarehouseDto(item.getId(), item.getName(), item.getLocation(), item.isActive()))
                .toList();
    }

    /**
     * Retrieves stock.
     *
     * @param variantId The variant id used to locate the target record.
     * @return The mapped DTO representation.
     */
    @Override
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER','B2B')")
    public InventorySummaryDto stock(UUID variantId) {
        List<InventoryLevelDto> levels = inventoryRepository.findByVariantId(variantId).stream()
                .map(this::toLevelDto)
                .toList();
        if (levels.isEmpty()) {
            ensureVariantExists(variantId);
        }
        return new InventorySummaryDto(variantId, levels);
    }

    /**
     * Adjusts stock.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryLevelDto adjust(InventoryAdjustRequest request) {
        ProductVariant variant = loadVariant(request.variantId());
        Warehouse warehouse = loadWarehouse(request.warehouseId());
        Inventory inventory = loadOrCreateInventory(variant, warehouse);
        int nextQuantity = inventory.getQuantity() + request.changeQuantity();
        if (nextQuantity < 0) {
            throw new BadRequestException("INVENTORY_NEGATIVE", "Adjustment would make stock negative");
        }
        if (nextQuantity < inventory.getReservedQuantity()) {
            throw new BadRequestException("INVENTORY_RESERVED_CONFLICT", "Stock cannot be lower than reserved quantity");
        }
        inventory.setQuantity(nextQuantity);
        if (request.reorderPoint() != null) {
            inventory.setReorderPoint(Math.max(0, request.reorderPoint()));
        }
        Inventory saved = inventoryRepository.save(inventory);
        logTransaction(
                saved.getVariant(),
                saved.getWarehouse(),
                request.changeQuantity(),
                InventoryTransactionType.ADJUSTMENT,
                null,
                request.reason()
        );
        return toLevelDto(saved);
    }

    /**
     * Reserves stock.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER','B2B')")
    public InventoryReservationDto reserve(InventoryReserveRequest request) {
        ProductVariant variant = loadVariant(request.variantId());
        Warehouse warehouse = loadWarehouse(request.warehouseId());
        Inventory inventory = inventoryRepository.findByVariantIdAndWarehouseId(variant.getId(), warehouse.getId()).orElse(null);
        int available = inventory == null ? 0 : availableQuantity(inventory);
        boolean backorder = available < request.quantity() && variant.getProduct().isAllowBackorder();
        if (available < request.quantity() && !backorder) {
            throw new BadRequestException("INVENTORY_INSUFFICIENT", "Insufficient available inventory");
        }

        InventoryReservation reservation = new InventoryReservation();
        reservation.setVariant(variant);
        reservation.setWarehouse(warehouse);
        reservation.setOrderId(request.orderId());
        reservation.setQuantity(request.quantity());
        reservation.setStatus(backorder ? InventoryReservationStatus.BACKORDERED : InventoryReservationStatus.RESERVED);
        reservation.setNote(request.note());
        InventoryReservation saved = inventoryReservationRepository.save(reservation);
        if (!backorder) {
            inventory.setReservedQuantity(inventory.getReservedQuantity() + request.quantity());
            inventoryRepository.save(inventory);
            logTransaction(
                    variant,
                    warehouse,
                    -request.quantity(),
                    InventoryTransactionType.RESERVE,
                    request.orderId(),
                    request.note()
            );
        } else {
            logTransaction(
                    variant,
                    warehouse,
                    0,
                    InventoryTransactionType.BACKORDER,
                    request.orderId(),
                    request.note()
            );
        }
        return toReservationDto(saved);
    }

    /**
     * Confirms reservation.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER','B2B')")
    public InventoryReservationDto confirm(InventoryReservationActionRequest request) {
        InventoryReservation reservation = loadReservation(request.reservationId());
        if (reservation.getStatus() != InventoryReservationStatus.RESERVED
                && reservation.getStatus() != InventoryReservationStatus.BACKORDERED) {
            throw new BadRequestException("RESERVATION_INVALID_STATE", "Reservation is not in confirmable state");
        }
        Inventory inventory = inventoryRepository.findByVariantIdAndWarehouseId(
                        reservation.getVariant().getId(),
                        reservation.getWarehouse().getId()
                )
                .orElse(null);

        if (reservation.getStatus() == InventoryReservationStatus.RESERVED) {
            if (inventory == null) {
                throw new NotFoundException("INVENTORY_NOT_FOUND", "Inventory not found");
            }
            if (inventory.getReservedQuantity() < reservation.getQuantity()) {
                throw new BadRequestException("RESERVATION_QUANTITY_INVALID", "Reserved quantity is inconsistent");
            }
            if (inventory.getQuantity() < reservation.getQuantity()) {
                throw new BadRequestException("INVENTORY_NEGATIVE", "Stock is lower than reservation quantity");
            }
            inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
            inventory.setQuantity(inventory.getQuantity() - reservation.getQuantity());
            inventoryRepository.save(inventory);
        } else {
            if (inventory == null) {
                throw new BadRequestException("INVENTORY_INSUFFICIENT", "Backordered stock is still unavailable");
            }
            int available = availableQuantity(inventory);
            if (available < reservation.getQuantity()) {
                throw new BadRequestException("INVENTORY_INSUFFICIENT", "Backordered stock is still unavailable");
            }
            inventory.setQuantity(inventory.getQuantity() - reservation.getQuantity());
            inventoryRepository.save(inventory);
        }

        reservation.setStatus(InventoryReservationStatus.CONFIRMED);
        if (request.note() != null && !request.note().isBlank()) {
            reservation.setNote(request.note().trim());
        }
        InventoryReservation saved = inventoryReservationRepository.save(reservation);
        logTransaction(
                reservation.getVariant(),
                reservation.getWarehouse(),
                -reservation.getQuantity(),
                InventoryTransactionType.CONFIRM,
                reservation.getOrderId(),
                reservation.getNote()
        );
        return toReservationDto(saved);
    }

    /**
     * Releases reservation.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER','B2B')")
    public InventoryReservationDto release(InventoryReservationActionRequest request) {
        InventoryReservation reservation = loadReservation(request.reservationId());
        if (reservation.getStatus() != InventoryReservationStatus.RESERVED
                && reservation.getStatus() != InventoryReservationStatus.BACKORDERED) {
            throw new BadRequestException("RESERVATION_INVALID_STATE", "Reservation is not in releasable state");
        }
        Inventory inventory = inventoryRepository.findByVariantIdAndWarehouseId(
                        reservation.getVariant().getId(),
                        reservation.getWarehouse().getId()
                )
                .orElse(null);
        boolean wasReserved = reservation.getStatus() == InventoryReservationStatus.RESERVED;
        if (wasReserved) {
            if (inventory == null) {
                throw new NotFoundException("INVENTORY_NOT_FOUND", "Inventory not found");
            }
            if (inventory.getReservedQuantity() < reservation.getQuantity()) {
                throw new BadRequestException("RESERVATION_QUANTITY_INVALID", "Reserved quantity is inconsistent");
            }
            inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
            inventoryRepository.save(inventory);
        }

        reservation.setStatus(InventoryReservationStatus.RELEASED);
        if (request.note() != null && !request.note().isBlank()) {
            reservation.setNote(request.note().trim());
        }
        InventoryReservation saved = inventoryReservationRepository.save(reservation);
        logTransaction(
                reservation.getVariant(),
                reservation.getWarehouse(),
                wasReserved ? reservation.getQuantity() : 0,
                InventoryTransactionType.RELEASE,
                reservation.getOrderId(),
                reservation.getNote()
        );
        return toReservationDto(saved);
    }

    /**
     * Checks availability.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER','B2B')")
    public InventoryCheckResultDto check(InventoryCheckRequest request) {
        List<InventoryCheckResultItemDto> items = request.items().stream()
                .map(this::checkItem)
                .toList();
        return new InventoryCheckResultDto(items);
    }

    /**
     * Checks item.
     *
     * @param request The request payload for this operation.
     * @return The result of check item.
     */
    private InventoryCheckResultItemDto checkItem(InventoryCheckItemRequest request) {
        ProductVariant variant = loadVariant(request.variantId());
        Inventory inventory = inventoryRepository.findByVariantIdAndWarehouseId(request.variantId(), request.warehouseId())
                .orElse(null);
        int available = inventory == null ? 0 : availableQuantity(inventory);
        boolean availableNow = available >= request.quantity();
        boolean backorder = !availableNow && variant.getProduct().isAllowBackorder();
        return new InventoryCheckResultItemDto(
                request.variantId(),
                request.warehouseId(),
                request.quantity(),
                available,
                availableNow || backorder,
                backorder
        );
    }

    /**
     * Retrieves available quantity.
     *
     * @param inventory The inventory value.
     * @return The result of available quantity.
     */
    private int availableQuantity(Inventory inventory) {
        return Math.max(0, inventory.getQuantity() - inventory.getReservedQuantity());
    }

    /**
     * Retrieves to level dto.
     *
     * @param inventory The inventory value.
     * @return The result of to level dto.
     */
    private InventoryLevelDto toLevelDto(Inventory inventory) {
        return new InventoryLevelDto(
                inventory.getId(),
                inventory.getVariant().getId(),
                inventory.getWarehouse().getId(),
                inventory.getWarehouse().getName(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                availableQuantity(inventory),
                inventory.getReorderPoint()
        );
    }

    /**
     * Retrieves to reservation dto.
     *
     * @param reservation The reservation value.
     * @return The result of to reservation dto.
     */
    private InventoryReservationDto toReservationDto(InventoryReservation reservation) {
        return new InventoryReservationDto(
                reservation.getId(),
                reservation.getVariant().getId(),
                reservation.getWarehouse().getId(),
                reservation.getOrderId(),
                reservation.getQuantity(),
                reservation.getStatus(),
                reservation.getNote(),
                reservation.getCreatedAt()
        );
    }

    /**
     * Retrieves load variant.
     *
     * @param variantId The variant id used to locate the target record.
     * @return The result of load variant.
     */
    private ProductVariant loadVariant(UUID variantId) {
        return productVariantRepository.findById(variantId)
                .orElseThrow(() -> new NotFoundException("VARIANT_NOT_FOUND", "Variant not found"));
    }

    /**
     * Retrieves load warehouse.
     *
     * @param warehouseId The warehouse id used to locate the target record.
     * @return The result of load warehouse.
     */
    private Warehouse loadWarehouse(UUID warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException("WAREHOUSE_NOT_FOUND", "Warehouse not found"));
        if (!warehouse.isActive()) {
            throw new BadRequestException("WAREHOUSE_INACTIVE", "Warehouse is inactive");
        }
        return warehouse;
    }

    /**
     * Retrieves load or create inventory.
     *
     * @param variant The variant value.
     * @param warehouse The warehouse value.
     * @return The result of load or create inventory.
     */
    private Inventory loadOrCreateInventory(ProductVariant variant, Warehouse warehouse) {
        return inventoryRepository.findByVariantIdAndWarehouseId(variant.getId(), warehouse.getId())
                .orElseGet(() -> {
                    Inventory created = new Inventory();
                    created.setVariant(variant);
                    created.setWarehouse(warehouse);
                    created.setQuantity(0);
                    created.setReservedQuantity(0);
                    created.setReorderPoint(0);
                    return created;
                });
    }

    /**
     * Retrieves load reservation.
     *
     * @param reservationId The reservation id used to locate the target record.
     * @return The result of load reservation.
     */
    private InventoryReservation loadReservation(UUID reservationId) {
        return inventoryReservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("RESERVATION_NOT_FOUND", "Reservation not found"));
    }

    /**
     * Executes ensure variant exists.
     *
     * @param variantId The variant id used to locate the target record.
     */
    private void ensureVariantExists(UUID variantId) {
        if (!productVariantRepository.existsById(variantId)) {
            throw new NotFoundException("VARIANT_NOT_FOUND", "Variant not found");
        }
    }

    /**
     * Logs transaction.
     *
     * @param variant The variant value.
     * @param warehouse The warehouse value.
     * @param changeQuantity The change quantity value.
     * @param type The type value.
     * @param orderId The order id used to locate the target record.
     * @param note The note value.
     */
    private void logTransaction(
            ProductVariant variant,
            Warehouse warehouse,
            int changeQuantity,
            InventoryTransactionType type,
            UUID orderId,
            String note
    ) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setVariant(variant);
        transaction.setWarehouse(warehouse);
        transaction.setChangeQuantity(changeQuantity);
        transaction.setType(type);
        transaction.setOrderId(orderId);
        transaction.setNote(note);
        inventoryTransactionRepository.save(transaction);
    }
}
