package com.noura.platform.service;

import com.noura.platform.commerce.api.v1.dto.inventory.StockAvailabilityDto;
import com.noura.platform.commerce.api.v1.dto.inventory.StockMovementDto;
import com.noura.platform.commerce.api.v1.dto.inventory.StockAdjustmentRequest;
import com.noura.platform.commerce.api.v1.dto.inventory.StockReceiveRequest;
import com.noura.platform.domain.enums.StockMovementType;
import com.noura.platform.dto.inventory.InventoryAdjustRequest;
import com.noura.platform.dto.inventory.InventoryCheckRequest;
import com.noura.platform.dto.inventory.InventoryCheckResultDto;
import com.noura.platform.dto.inventory.InventoryLevelDto;
import com.noura.platform.dto.inventory.InventoryReservationActionRequest;
import com.noura.platform.dto.inventory.InventoryReservationDto;
import com.noura.platform.dto.inventory.InventoryReserveRequest;
import com.noura.platform.dto.inventory.InventorySummaryDto;
import com.noura.platform.dto.inventory.VariantUnitDeductionResultDto;
import com.noura.platform.dto.inventory.WarehouseDto;
import com.noura.platform.dto.inventory.WarehouseRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface UnifiedInventoryService {
    WarehouseDto createWarehouse(WarehouseRequest request);

    List<WarehouseDto> warehouses();

    InventorySummaryDto stock(UUID variantId);

    InventoryLevelDto adjust(InventoryAdjustRequest request);

    InventoryReservationDto reserve(InventoryReserveRequest request);

    InventoryReservationDto confirm(InventoryReservationActionRequest request);

    InventoryReservationDto release(InventoryReservationActionRequest request);

    InventoryCheckResultDto check(InventoryCheckRequest request);

    Page<StockMovementDto> listCommerceMovements(LocalDate from,
                                                 LocalDate to,
                                                 Long productId,
                                                 StockMovementType type,
                                                 Pageable pageable);

    StockAvailabilityDto getCommerceAvailability(Long productId);

    StockAvailabilityDto adjustCommerceStock(StockAdjustmentRequest request);

    StockAvailabilityDto receiveCommerceStock(StockReceiveRequest request);

    VariantUnitDeductionResultDto deductCommerceVariantUnitStock(Long sellUnitId, java.math.BigDecimal qty);
}
