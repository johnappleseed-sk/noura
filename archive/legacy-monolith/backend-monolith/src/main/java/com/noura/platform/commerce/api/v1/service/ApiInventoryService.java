package com.noura.platform.commerce.api.v1.service;

import com.noura.platform.commerce.api.v1.dto.inventory.StockAdjustmentRequest;
import com.noura.platform.commerce.api.v1.dto.inventory.StockAvailabilityDto;
import com.noura.platform.commerce.api.v1.dto.inventory.StockMovementDto;
import com.noura.platform.commerce.api.v1.dto.inventory.StockReceiveRequest;
import com.noura.platform.domain.enums.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ApiInventoryService {
    Page<StockMovementDto> listMovements(LocalDate from,
                                         LocalDate to,
                                         Long productId,
                                         StockMovementType type,
                                         Pageable pageable);

    StockAvailabilityDto getAvailability(Long productId);

    StockAvailabilityDto adjustStock(StockAdjustmentRequest request);

    StockAvailabilityDto receiveStock(StockReceiveRequest request);
}
