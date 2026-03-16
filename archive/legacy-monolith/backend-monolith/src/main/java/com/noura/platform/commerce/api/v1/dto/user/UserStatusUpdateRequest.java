package com.noura.platform.commerce.api.v1.dto.user;

import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(
        @NotNull(message = "active is required")
        Boolean active
) {
}
