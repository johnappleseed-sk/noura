package com.noura.platform.service;

import com.noura.platform.dto.payment.CreatePaymentRequest;
import com.noura.platform.dto.payment.MockPaymentWebhookRequest;
import com.noura.platform.dto.payment.PaymentResponse;

import java.util.UUID;

public interface PaymentService {
    PaymentResponse createPayment(CreatePaymentRequest request);

    PaymentResponse getPayment(UUID paymentId);

    PaymentResponse processMockWebhook(MockPaymentWebhookRequest request);
}
