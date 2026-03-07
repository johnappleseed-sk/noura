package com.noura.platform.mapper;

import com.noura.platform.domain.entity.B2BCompanyProfile;
import com.noura.platform.dto.user.CompanyProfileDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CompanyMapper {
    /**
     * Maps source data to CompanyProfileDto.
     *
     * @param entity The source object to transform.
     * @return The mapped DTO representation.
     */
    CompanyProfileDto toDto(B2BCompanyProfile entity);
}
