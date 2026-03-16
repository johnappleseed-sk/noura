package com.noura.platform.dto.user;

import com.noura.platform.domain.enums.RoleType;

import java.util.Set;
import java.util.UUID;

public record UserProfileDto(
        UUID id,
        String fullName,
        String email,
        String phone,
        Set<RoleType> roles,
        boolean enabled,
        UUID preferredStoreId
) {
}
