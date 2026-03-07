package com.noura.platform.mapper;

import com.noura.platform.domain.entity.Order;
import com.noura.platform.domain.entity.OrderItem;
import com.noura.platform.dto.order.OrderDto;
import com.noura.platform.dto.order.OrderItemDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    /**
     * Maps source data to OrderDto.
     *
     * @param entity The source object to transform.
     * @return The mapped DTO representation.
     */
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "storeId", source = "store.id")
    @Mapping(target = "items", ignore = true)
    OrderDto toDto(Order entity);

    /**
     * Maps source data to OrderItemDto.
     *
     * @param entity The source object to transform.
     * @return The mapped DTO representation.
     */
    @Mapping(target = "productId", source = "product.id")
    OrderItemDto toItemDto(OrderItem entity);
}
