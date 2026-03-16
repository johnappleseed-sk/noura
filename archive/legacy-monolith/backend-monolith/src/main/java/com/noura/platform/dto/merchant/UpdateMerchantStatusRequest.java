package com.noura.platform.dto.merchant;

import com.noura.platform.domain.enums.MerchantStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateMerchantStatusRequest(
        @NotNull MerchantStatus status
) {
}
