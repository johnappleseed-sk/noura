package com.noura.platform.inventory.service;

import com.noura.platform.inventory.dto.warehouse.WarehouseBinFilter;
import com.noura.platform.inventory.dto.warehouse.WarehouseBinRequest;
import com.noura.platform.inventory.dto.warehouse.WarehouseBinResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WarehouseBinService {

    WarehouseBinResponse createBin(String warehouseId, WarehouseBinRequest request);

    WarehouseBinResponse updateBin(String binId, WarehouseBinRequest request);

    WarehouseBinResponse getBin(String binId);

    Page<WarehouseBinResponse> listBins(WarehouseBinFilter filter, Pageable pageable);

    void deleteBin(String binId);
}
