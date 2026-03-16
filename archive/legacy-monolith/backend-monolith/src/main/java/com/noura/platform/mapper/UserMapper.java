package com.noura.platform.mapper;

import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.dto.user.UserProfileDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Maps source data to UserProfileDto.
     *
     * @param entity The source object to transform.
     * @return The mapped DTO representation.
     */
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "preferredStoreId", source = "preferredStoreId")
    UserProfileDto toDto(UserAccount entity);
}
