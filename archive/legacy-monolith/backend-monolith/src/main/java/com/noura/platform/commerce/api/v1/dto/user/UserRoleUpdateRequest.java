package com.noura.platform.commerce.api.v1.dto.user;

import com.noura.platform.commerce.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequest(
        @NotNull(message = "role is required")
        UserRole role
) {
}
