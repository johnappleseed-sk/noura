package com.noura.platform.dto.payment;

import com.noura.platform.domain.enums.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MockPaymentWebhookRequest(
        @NotBlank @Size(max = 64) String paymentReference,
        @NotNull PaymentStatus status,
        @Size(max = 128) String providerTransactionId,
        @Size(max = 500) String failureReason
) {
}
