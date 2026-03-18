package com.noura.shipping.dto.network;

import com.noura.shipping.domain.enums.StoreStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Store status change payload aligned with admin-web.
 */
public record StoreStatusUpdateRequest(
        @NotNull StoreStatus status
) {
}
