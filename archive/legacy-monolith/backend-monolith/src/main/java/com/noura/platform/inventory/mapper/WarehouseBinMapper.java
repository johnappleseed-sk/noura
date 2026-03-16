package com.noura.platform.inventory.mapper;

import com.noura.platform.inventory.domain.WarehouseBin;
import com.noura.platform.inventory.dto.warehouse.WarehouseBinResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = WarehouseMapper.class)
public interface WarehouseBinMapper {

    @Mapping(target = "warehouse", source = "warehouse")
    WarehouseBinResponse toResponse(WarehouseBin warehouseBin);
}
