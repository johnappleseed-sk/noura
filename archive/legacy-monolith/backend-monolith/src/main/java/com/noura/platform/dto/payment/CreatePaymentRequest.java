package com.noura.platform.dto.payment;

import com.noura.platform.domain.enums.PaymentMethodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID orderId,
        @NotNull PaymentMethodType methodType,
        @NotBlank @Size(max = 64) String providerCode,
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currencyCode must be a 3-letter ISO code") String currencyCode,
        @Size(max = 64) String paymentMethod,
        @Size(max = 64) String provider,
        @Size(max = 128) String providerReference
) {
    public CreatePaymentRequest {
        providerCode = normalize(providerCode);
        currencyCode = normalize(currencyCode);
        paymentMethod = normalize(paymentMethod);
        provider = normalize(provider);
        providerReference = normalize(providerReference);
    }

    public CreatePaymentRequest(String paymentMethod, String provider, String providerReference) {
        this(null, null, provider, null, paymentMethod, provider, providerReference);
    }

    public String resolvedProviderCode() {
        return providerCode != null ? providerCode : provider;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
