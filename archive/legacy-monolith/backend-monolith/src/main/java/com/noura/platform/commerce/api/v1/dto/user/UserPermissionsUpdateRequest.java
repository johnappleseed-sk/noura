package com.noura.platform.commerce.api.v1.dto.user;

import com.noura.platform.commerce.entity.Permission;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UserPermissionsUpdateRequest(
        @NotNull(message = "permissions is required")
        Set<Permission> permissions
) {
}
