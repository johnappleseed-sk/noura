package com.noura.platform.dto.user;

import com.noura.platform.domain.enums.RoleType;

import java.util.Set;

public record AdminUserUpdateRequest(
        Set<RoleType> roles,
        Boolean enabled
) {
}
