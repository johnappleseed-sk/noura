package com.noura.platform.mapper;

import com.noura.platform.domain.entity.ApprovalRequest;
import com.noura.platform.dto.user.ApprovalDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApprovalMapper {

    /**
     * Maps source data to ApprovalDto.
     *
     * @param entity The source object to transform.
     * @return The mapped DTO representation.
     */
    @Mapping(target = "requesterId", source = "requester.id")
    @Mapping(target = "orderId", source = "order.id")
    ApprovalDto toDto(ApprovalRequest entity);
}
