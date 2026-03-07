package com.noura.platform.dto.user;

import com.noura.platform.domain.enums.PaymentMethodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentMethodRequest(
        @NotNull PaymentMethodType methodType,
        @NotBlank String provider,
        @NotBlank String tokenizedReference,
        boolean defaultMethod
) {
}
