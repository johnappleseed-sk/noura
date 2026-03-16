package com.noura.platform.mapper;

import com.noura.platform.domain.entity.PaymentMethod;
import com.noura.platform.dto.user.PaymentMethodDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMethodMapper {
    /**
     * Maps source data to PaymentMethodDto.
     *
     * @param entity The source object to transform.
     * @return The mapped DTO representation.
     */
    @Mapping(target = "defaultMethod", source = "defaultMethod")
    PaymentMethodDto toDto(PaymentMethod entity);
}
