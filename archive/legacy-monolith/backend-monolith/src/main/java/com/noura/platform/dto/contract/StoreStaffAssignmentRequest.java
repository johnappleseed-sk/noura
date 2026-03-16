package com.noura.platform.dto.contract;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StoreStaffAssignmentRequest(
        @NotNull UUID userId,
        String roleCode,
        Boolean active
        ) {
}

