package com.noura.platform.service;

import com.noura.platform.dto.payment.CreatePaymentRequest;
import com.noura.platform.dto.payment.PaymentTransactionResult;
import com.noura.platform.dto.user.PaymentMethodDto;
import com.noura.platform.dto.user.PaymentMethodRequest;

import java.util.List;
import java.util.UUID;

/**
 * Canonical payment facade — Stage 3 consolidation.
 *
 * Delegates to:
 *   - platform UserAccountService (payment method CRUD for platform accounts)
 *   - commerce StorefrontPaymentService (storefront payment transactions)
 *
 * Legacy references preserved for traceability:
 *   - com.noura.platform.service.UserAccountService (payment-method portion)
 *   - com.noura.platform.commerce.payments.application.StorefrontPaymentService
 *   - com.noura.platform.commerce.payments.application.PaymentGateway
 *   - com.noura.platform.commerce.payments.application.StripePaymentGateway
 *   - com.noura.platform.commerce.payments.application.StubPaymentGateway
 */
public interface UnifiedPaymentService {

    // --- Platform payment method management ---

    List<PaymentMethodDto> listPaymentMethods();

    PaymentMethodDto addPaymentMethod(PaymentMethodRequest request);

    PaymentMethodDto updatePaymentMethod(UUID paymentMethodId, PaymentMethodRequest request);

    void deletePaymentMethod(UUID paymentMethodId);

    // --- Storefront (commerce) payment transactions ---

    List<PaymentTransactionResult> listStorefrontPayments(Long customerId, Long orderId);

    PaymentTransactionResult createStorefrontPayment(
            Long customerId,
            Long orderId,
            CreatePaymentRequest request
    );

    PaymentTransactionResult captureStorefrontPayment(Long customerId, Long orderId, Long paymentId);
}
