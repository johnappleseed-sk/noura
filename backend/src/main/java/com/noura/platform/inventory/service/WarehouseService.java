package com.noura.platform.inventory.service;

import com.noura.platform.inventory.dto.warehouse.WarehouseFilter;
import com.noura.platform.inventory.dto.warehouse.WarehouseRequest;
import com.noura.platform.inventory.dto.warehouse.WarehouseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WarehouseService {

    WarehouseResponse createWarehouse(WarehouseRequest request);

    WarehouseResponse updateWarehouse(String warehouseId, WarehouseRequest request);

    WarehouseResponse getWarehouse(String warehouseId);

    Page<WarehouseResponse> listWarehouses(WarehouseFilter filter, Pageable pageable);

    void deleteWarehouse(String warehouseId);
}
