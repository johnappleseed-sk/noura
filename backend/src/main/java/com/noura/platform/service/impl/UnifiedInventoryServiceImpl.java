package com.noura.platform.service.impl;

import com.noura.platform.commerce.api.v1.dto.inventory.StockAdjustmentRequest;
import com.noura.platform.commerce.api.v1.dto.inventory.StockAvailabilityDto;
import com.noura.platform.commerce.api.v1.dto.inventory.StockMovementDto;
import com.noura.platform.commerce.api.v1.dto.inventory.StockReceiveRequest;
import com.noura.platform.commerce.api.v1.service.ApiInventoryService;
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
import com.noura.platform.service.InventoryService;
import com.noura.platform.service.UnifiedInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UnifiedInventoryServiceImpl implements UnifiedInventoryService {

    private final InventoryService platformInventoryService;
    private final ObjectProvider<ApiInventoryService> commerceInventoryServiceProvider;
    private final ObjectProvider<com.noura.platform.commerce.service.InventoryService> commerceVariantInventoryServiceProvider;

    @Override
    public WarehouseDto createWarehouse(WarehouseRequest request) {
        return platformInventoryService.createWarehouse(request);
    }

    @Override
    public List<WarehouseDto> warehouses() {
        return platformInventoryService.warehouses();
    }

    @Override
    public InventorySummaryDto stock(UUID variantId) {
        return platformInventoryService.stock(variantId);
    }

    @Override
    public InventoryLevelDto adjust(InventoryAdjustRequest request) {
        return platformInventoryService.adjust(request);
    }

    @Override
    public InventoryReservationDto reserve(InventoryReserveRequest request) {
        return platformInventoryService.reserve(request);
    }

    @Override
    public InventoryReservationDto confirm(InventoryReservationActionRequest request) {
        return platformInventoryService.confirm(request);
    }

    @Override
    public InventoryReservationDto release(InventoryReservationActionRequest request) {
        return platformInventoryService.release(request);
    }

    @Override
    public InventoryCheckResultDto check(InventoryCheckRequest request) {
        return platformInventoryService.check(request);
    }

    @Override
    public Page<StockMovementDto> listCommerceMovements(
            LocalDate from,
            LocalDate to,
            Long productId,
            StockMovementType type,
            Pageable pageable
    ) {
        return commerceInventoryService().listMovements(from, to, productId, type, pageable);
    }

    @Override
    public StockAvailabilityDto getCommerceAvailability(Long productId) {
        return commerceInventoryService().getAvailability(productId);
    }

    @Override
    public StockAvailabilityDto adjustCommerceStock(StockAdjustmentRequest request) {
        return commerceInventoryService().adjustStock(request);
    }

    @Override
    public StockAvailabilityDto receiveCommerceStock(StockReceiveRequest request) {
        return commerceInventoryService().receiveStock(request);
    }

    private ApiInventoryService commerceInventoryService() {
        ApiInventoryService service = commerceInventoryServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("Legacy commerce inventory service is not active in the current runtime profile.");
        }
        return service;
    }

    private com.noura.platform.commerce.service.InventoryService commerceVariantInventoryService() {
        com.noura.platform.commerce.service.InventoryService service = commerceVariantInventoryServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("Commerce variant inventory service is not active in the current runtime profile.");
        }
        return service;
    }

    @Override
    public VariantUnitDeductionResultDto deductCommerceVariantUnitStock(
            Long sellUnitId, java.math.BigDecimal qty) {
        var result = commerceVariantInventoryService().deductVariantUnitStock(sellUnitId, qty);
        return new VariantUnitDeductionResultDto(
                result.variantId(), result.sellUnitId(), result.soldQty(),
                result.deductedBaseQty(), result.remainingBaseQty()
        );
    }
}
