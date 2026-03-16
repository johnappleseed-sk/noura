package com.noura.platform.service.impl;

import com.noura.platform.commerce.payments.application.StorefrontPaymentService;
import com.noura.platform.dto.payment.CreatePaymentRequest;
import com.noura.platform.dto.payment.PaymentTransactionResult;
import com.noura.platform.dto.user.PaymentMethodDto;
import com.noura.platform.dto.user.PaymentMethodRequest;
import com.noura.platform.service.UnifiedPaymentService;
import com.noura.platform.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Canonical payment facade implementation — Stage 3 consolidation.
 *
 * All payment-related calls across platform and commerce trees
 * are routed through this single service boundary.
 */
@Service
@RequiredArgsConstructor
public class UnifiedPaymentServiceImpl implements UnifiedPaymentService {

    private final UserAccountService userAccountService;
    private final ObjectProvider<StorefrontPaymentService> storefrontPaymentServiceProvider;

    // --- Platform payment method management ---

    @Override
    public List<PaymentMethodDto> listPaymentMethods() {
        return userAccountService.listPaymentMethods();
    }

    @Override
    public PaymentMethodDto addPaymentMethod(PaymentMethodRequest request) {
        return userAccountService.addPaymentMethod(request);
    }

    @Override
    public PaymentMethodDto updatePaymentMethod(UUID paymentMethodId, PaymentMethodRequest request) {
        return userAccountService.updatePaymentMethod(paymentMethodId, request);
    }

    @Override
    public void deletePaymentMethod(UUID paymentMethodId) {
        userAccountService.deletePaymentMethod(paymentMethodId);
    }

    // --- Storefront (commerce) payment transactions ---

    @Override
    public List<PaymentTransactionResult> listStorefrontPayments(Long customerId, Long orderId) {
        return storefrontPaymentService().listForOrder(customerId, orderId);
    }

    @Override
    public PaymentTransactionResult createStorefrontPayment(
            Long customerId,
            Long orderId,
            CreatePaymentRequest request
    ) {
        return storefrontPaymentService().createInitialPayment(customerId, orderId, request);
    }

    @Override
    public PaymentTransactionResult captureStorefrontPayment(
            Long customerId,
            Long orderId,
            Long paymentId
    ) {
        return storefrontPaymentService().capture(customerId, orderId, paymentId);
    }

    private StorefrontPaymentService storefrontPaymentService() {
        StorefrontPaymentService service = storefrontPaymentServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("Legacy storefront payment service is not active in the current runtime profile.");
        }
        return service;
    }
}
