package com.noura.platform.mapper;

import com.noura.platform.domain.entity.Store;
import com.noura.platform.dto.store.StoreDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StoreMapper {

    /**
     * Maps source data to StoreDto.
     *
     * @param entity The source object to transform.
     * @return The mapped DTO representation.
     */
    @Mapping(target = "distanceKm", ignore = true)
    @Mapping(target = "openNow", ignore = true)
    StoreDto toDto(Store entity);
}
