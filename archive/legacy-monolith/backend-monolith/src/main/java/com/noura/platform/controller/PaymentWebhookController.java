package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.payment.MockPaymentWebhookRequest;
import com.noura.platform.dto.payment.PaymentResponse;
import com.noura.platform.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/payments/webhooks", "${app.api.version-prefix:/api/v1}/payments/webhooks"})
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/mock")
    public ApiResponse<PaymentResponse> processMockWebhook(
            @Valid @RequestBody MockPaymentWebhookRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Mock payment webhook processed", paymentService.processMockWebhook(request), http.getRequestURI());
    }
}
