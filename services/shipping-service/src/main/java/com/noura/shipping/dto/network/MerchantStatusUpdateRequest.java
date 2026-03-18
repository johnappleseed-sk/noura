package com.noura.shipping.dto.network;

import com.noura.shipping.domain.enums.MerchantStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Merchant status change payload aligned with admin-web.
 */
public record MerchantStatusUpdateRequest(
        @NotNull MerchantStatus status
) {
}
