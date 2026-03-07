package com.noura.platform.mapper;

import com.noura.platform.domain.entity.Notification;
import com.noura.platform.dto.notification.NotificationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    /**
     * Maps source data to NotificationDto.
     *
     * @param entity The source object to transform.
     * @return The mapped DTO representation.
     */
    @Mapping(target = "targetUserId", source = "targetUser.id")
    NotificationDto toDto(Notification entity);
}
