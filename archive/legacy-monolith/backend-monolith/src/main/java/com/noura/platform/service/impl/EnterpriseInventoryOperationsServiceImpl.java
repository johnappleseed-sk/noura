package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.Inventory;
import com.noura.platform.domain.entity.InventoryReservation;
import com.noura.platform.domain.entity.InventoryRestockSchedule;
import com.noura.platform.domain.entity.InventoryTransaction;
import com.noura.platform.domain.entity.InventoryTransfer;
import com.noura.platform.domain.entity.ProductVariant;
import com.noura.platform.domain.entity.Warehouse;
import com.noura.platform.domain.enums.InventoryRestockScheduleStatus;
import com.noura.platform.domain.enums.InventoryTransactionType;
import com.noura.platform.domain.enums.InventoryTransferStatus;
import com.noura.platform.dto.inventory.InventoryReservationViewDto;
import com.noura.platform.dto.inventory.InventoryRestockScheduleDto;
import com.noura.platform.dto.inventory.InventoryRestockScheduleRequest;
import com.noura.platform.dto.inventory.InventoryTransferDto;
import com.noura.platform.dto.inventory.InventoryTransferRequest;
import com.noura.platform.dto.inventory.LowStockAlertDto;
import com.noura.platform.repository.InventoryRepository;
import com.noura.platform.repository.InventoryReservationRepository;
import com.noura.platform.repository.InventoryRestockScheduleRepository;
import com.noura.platform.repository.InventoryTransactionRepository;
import com.noura.platform.repository.InventoryTransferRepository;
import com.noura.platform.repository.ProductVariantRepository;
import com.noura.platform.repository.WarehouseRepository;
import com.noura.platform.security.SecurityUtils;
import com.noura.platform.service.EnterpriseInventoryOperationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnterpriseInventoryOperationsServiceImpl implements EnterpriseInventoryOperationsService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final InventoryTransferRepository inventoryTransferRepository;
    private final InventoryRestockScheduleRepository inventoryRestockScheduleRepository;
    private final ProductVariantRepository productVariantRepository;
    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    public InventoryTransferDto createTransfer(InventoryTransferRequest request) {
        if (request.fromWarehouseId().equals(request.toWarehouseId())) {
            throw new BadRequestException("INVENTORY_TRANSFER_INVALID", "Source and target warehouses must be different.");
        }
        ProductVariant variant = requireVariant(request.variantId());
        Warehouse fromWarehouse = requireWarehouse(request.fromWarehouseId());
        Warehouse toWarehouse = requireWarehouse(request.toWarehouseId());

        InventoryTransfer transfer = new InventoryTransfer();
        transfer.setVariant(variant);
        transfer.setFromWarehouse(fromWarehouse);
        transfer.setToWarehouse(toWarehouse);
        transfer.setQuantity(request.quantity());
        transfer.setScheduledFor(request.scheduledFor());
        transfer.setRequestedBy(SecurityUtils.currentEmail());
        transfer.setNote(trimToNull(request.note()));

        Instant now = Instant.now();
        boolean immediate = request.scheduledFor() == null || !request.scheduledFor().isAfter(now);
        if (immediate) {
            // Transfers mutate inventory atomically so available stock stays deterministic across warehouses.
            Inventory source = inventoryRepository.findByVariantIdAndWarehouseId(variant.getId(), fromWarehouse.getId())
                    .orElseThrow(() -> new NotFoundException("INVENTORY_NOT_FOUND", "Source inventory not found."));
            int available = Math.max(0, source.getQuantity() - source.getReservedQuantity());
            if (available < request.quantity()) {
                throw new BadRequestException("INVENTORY_TRANSFER_INSUFFICIENT", "Insufficient available stock to transfer.");
            }
            source.setQuantity(source.getQuantity() - request.quantity());
            inventoryRepository.save(source);

            Inventory target = inventoryRepository.findByVariantIdAndWarehouseId(variant.getId(), toWarehouse.getId())
                    .orElseGet(() -> createInventory(variant, toWarehouse));
            target.setQuantity(target.getQuantity() + request.quantity());
            inventoryRepository.save(target);

            transfer.setStatus(InventoryTransferStatus.COMPLETED);
            transfer.setCompletedAt(now);
            recordTransaction(variant, fromWarehouse, -request.quantity(), InventoryTransactionType.TRANSFER_OUT, transfer.getNote());
            recordTransaction(variant, toWarehouse, request.quantity(), InventoryTransactionType.TRANSFER_IN, transfer.getNote());
        } else {
            transfer.setStatus(InventoryTransferStatus.SCHEDULED);
        }
        return toDto(inventoryTransferRepository.save(transfer));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryTransferDto> listTransfers() {
        return inventoryTransferRepository.findAll().stream()
                .sorted(Comparator.comparing(InventoryTransfer::getCreatedAt).reversed())
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public InventoryRestockScheduleDto createRestockSchedule(InventoryRestockScheduleRequest request) {
        ProductVariant variant = requireVariant(request.variantId());
        Warehouse warehouse = requireWarehouse(request.warehouseId());
        InventoryRestockSchedule schedule = new InventoryRestockSchedule();
        schedule.setVariant(variant);
        schedule.setWarehouse(warehouse);
        schedule.setTargetQuantity(request.targetQuantity());
        schedule.setScheduledFor(request.scheduledFor());
        schedule.setRequestedBy(SecurityUtils.currentEmail());
        schedule.setNote(trimToNull(request.note()));
        schedule.setStatus(InventoryRestockScheduleStatus.SCHEDULED);
        return toDto(inventoryRestockScheduleRepository.save(schedule));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryRestockScheduleDto> listRestockSchedules() {
        return inventoryRestockScheduleRepository.findAll().stream()
                .sorted(Comparator.comparing(InventoryRestockSchedule::getScheduledFor))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LowStockAlertDto> listLowStockAlerts() {
        return inventoryRepository.findAll().stream()
                .filter(inventory -> inventory.getReorderPoint() > 0)
                .map(this::toLowStockAlert)
                .filter(alert -> alert.availableQuantity() <= alert.reorderPoint())
                .sorted(Comparator.comparingInt(LowStockAlertDto::availableQuantity))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryReservationViewDto> listReservations() {
        return inventoryReservationRepository.findAll().stream()
                .sorted(Comparator.comparing(InventoryReservation::getCreatedAt).reversed())
                .map(this::toReservationDto)
                .toList();
    }

    private ProductVariant requireVariant(UUID variantId) {
        return productVariantRepository.findById(variantId)
                .orElseThrow(() -> new NotFoundException("VARIANT_NOT_FOUND", "Variant not found."));
    }

    private Warehouse requireWarehouse(UUID warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException("WAREHOUSE_NOT_FOUND", "Warehouse not found."));
    }

    private Inventory createInventory(ProductVariant variant, Warehouse warehouse) {
        Inventory inventory = new Inventory();
        inventory.setVariant(variant);
        inventory.setWarehouse(warehouse);
        inventory.setQuantity(0);
        inventory.setReservedQuantity(0);
        inventory.setReorderPoint(0);
        return inventory;
    }

    private void recordTransaction(ProductVariant variant,
                                   Warehouse warehouse,
                                   int changeQuantity,
                                   InventoryTransactionType type,
                                   String note) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setVariant(variant);
        transaction.setWarehouse(warehouse);
        transaction.setChangeQuantity(changeQuantity);
        transaction.setType(type);
        transaction.setNote(note);
        inventoryTransactionRepository.save(transaction);
    }

    private InventoryTransferDto toDto(InventoryTransfer transfer) {
        return new InventoryTransferDto(
                transfer.getId(),
                transfer.getVariant().getId(),
                transfer.getFromWarehouse().getId(),
                transfer.getFromWarehouse().getName(),
                transfer.getToWarehouse().getId(),
                transfer.getToWarehouse().getName(),
                transfer.getQuantity(),
                transfer.getStatus(),
                transfer.getScheduledFor(),
                transfer.getCompletedAt(),
                transfer.getRequestedBy(),
                transfer.getNote(),
                transfer.getCreatedAt()
        );
    }

    private InventoryRestockScheduleDto toDto(InventoryRestockSchedule schedule) {
        return new InventoryRestockScheduleDto(
                schedule.getId(),
                schedule.getVariant().getId(),
                schedule.getWarehouse().getId(),
                schedule.getWarehouse().getName(),
                schedule.getTargetQuantity(),
                schedule.getStatus(),
                schedule.getScheduledFor(),
                schedule.getRequestedBy(),
                schedule.getNote(),
                schedule.getCreatedAt()
        );
    }

    private LowStockAlertDto toLowStockAlert(Inventory inventory) {
        return new LowStockAlertDto(
                inventory.getId(),
                inventory.getVariant().getId(),
                inventory.getWarehouse().getId(),
                inventory.getWarehouse().getName(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                Math.max(0, inventory.getQuantity() - inventory.getReservedQuantity()),
                inventory.getReorderPoint()
        );
    }

    private InventoryReservationViewDto toReservationDto(InventoryReservation reservation) {
        return new InventoryReservationViewDto(
                reservation.getId(),
                reservation.getVariant().getId(),
                reservation.getWarehouse().getId(),
                reservation.getWarehouse().getName(),
                reservation.getOrderId(),
                reservation.getQuantity(),
                reservation.getStatus(),
                reservation.getNote(),
                reservation.getCreatedAt()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
