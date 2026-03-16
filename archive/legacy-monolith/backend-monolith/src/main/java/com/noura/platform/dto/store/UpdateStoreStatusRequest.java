package com.noura.platform.dto.store;

import com.noura.platform.domain.enums.StoreStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStoreStatusRequest(
        @NotNull StoreStatus status
) {
}
