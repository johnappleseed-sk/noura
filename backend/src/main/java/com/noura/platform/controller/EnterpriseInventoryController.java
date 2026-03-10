package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.inventory.InventoryReservationViewDto;
import com.noura.platform.dto.inventory.InventoryRestockScheduleDto;
import com.noura.platform.dto.inventory.InventoryRestockScheduleRequest;
import com.noura.platform.dto.inventory.InventoryTransferDto;
import com.noura.platform.dto.inventory.InventoryTransferRequest;
import com.noura.platform.dto.inventory.LowStockAlertDto;
import com.noura.platform.service.EnterpriseInventoryOperationsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/admin/inventory")
@PreAuthorize("hasRole('ADMIN')")
public class EnterpriseInventoryController {

    private final EnterpriseInventoryOperationsService enterpriseInventoryOperationsService;

    @GetMapping("/alerts/low-stock")
    public ApiResponse<List<LowStockAlertDto>> lowStockAlerts(HttpServletRequest http) {
        return ApiResponse.ok("Low stock alerts", enterpriseInventoryOperationsService.listLowStockAlerts(), http.getRequestURI());
    }

    @GetMapping("/reservations")
    public ApiResponse<List<InventoryReservationViewDto>> reservations(HttpServletRequest http) {
        return ApiResponse.ok("Inventory reservations", enterpriseInventoryOperationsService.listReservations(), http.getRequestURI());
    }

    @GetMapping("/transfers")
    public ApiResponse<List<InventoryTransferDto>> transfers(HttpServletRequest http) {
        return ApiResponse.ok("Inventory transfers", enterpriseInventoryOperationsService.listTransfers(), http.getRequestURI());
    }

    @PostMapping("/transfers")
    public ResponseEntity<ApiResponse<InventoryTransferDto>> createTransfer(
            @Valid @RequestBody InventoryTransferRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Inventory transfer created", enterpriseInventoryOperationsService.createTransfer(request), http.getRequestURI()));
    }

    @GetMapping("/restock-schedules")
    public ApiResponse<List<InventoryRestockScheduleDto>> restockSchedules(HttpServletRequest http) {
        return ApiResponse.ok("Inventory restock schedules", enterpriseInventoryOperationsService.listRestockSchedules(), http.getRequestURI());
    }

    @PostMapping("/restock-schedules")
    public ResponseEntity<ApiResponse<InventoryRestockScheduleDto>> createRestockSchedule(
            @Valid @RequestBody InventoryRestockScheduleRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Inventory restock schedule created", enterpriseInventoryOperationsService.createRestockSchedule(request), http.getRequestURI()));
    }
}
