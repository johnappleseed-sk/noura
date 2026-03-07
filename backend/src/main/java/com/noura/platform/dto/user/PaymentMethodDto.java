package com.noura.platform.dto.user;

import com.noura.platform.domain.enums.PaymentMethodType;

import java.util.UUID;

public record PaymentMethodDto(
        UUID id,
        PaymentMethodType methodType,
        String provider,
        String tokenizedReference,
        boolean defaultMethod
) {
}
