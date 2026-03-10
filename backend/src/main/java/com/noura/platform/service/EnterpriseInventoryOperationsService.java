package com.noura.platform.service;

import com.noura.platform.dto.inventory.InventoryReservationViewDto;
import com.noura.platform.dto.inventory.InventoryRestockScheduleDto;
import com.noura.platform.dto.inventory.InventoryRestockScheduleRequest;
import com.noura.platform.dto.inventory.InventoryTransferDto;
import com.noura.platform.dto.inventory.InventoryTransferRequest;
import com.noura.platform.dto.inventory.LowStockAlertDto;

import java.util.List;

public interface EnterpriseInventoryOperationsService {
    InventoryTransferDto createTransfer(InventoryTransferRequest request);

    List<InventoryTransferDto> listTransfers();

    InventoryRestockScheduleDto createRestockSchedule(InventoryRestockScheduleRequest request);

    List<InventoryRestockScheduleDto> listRestockSchedules();

    List<LowStockAlertDto> listLowStockAlerts();

    List<InventoryReservationViewDto> listReservations();
}
