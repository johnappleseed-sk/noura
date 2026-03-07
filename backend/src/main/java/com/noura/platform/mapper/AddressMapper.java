package com.noura.platform.mapper;

import com.noura.platform.domain.entity.Address;
import com.noura.platform.dto.user.AddressDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AddressMapper {
    /**
     * Maps source data to AddressDto.
     *
     * @param entity The source object to transform.
     * @return The mapped DTO representation.
     */
    @Mapping(target = "defaultAddress", source = "defaultAddress")
    AddressDto toDto(Address entity);
}
