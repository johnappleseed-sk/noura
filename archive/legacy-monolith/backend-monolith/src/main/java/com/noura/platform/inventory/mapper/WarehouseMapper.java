package com.noura.platform.inventory.mapper;

import com.noura.platform.inventory.domain.Warehouse;
import com.noura.platform.inventory.dto.warehouse.WarehouseResponse;
import com.noura.platform.inventory.dto.warehouse.WarehouseSummaryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WarehouseMapper {

    WarehouseResponse toResponse(Warehouse warehouse);

    WarehouseSummaryResponse toSummary(Warehouse warehouse);
}
