package com.noura.platform.commerce.payments.web;

import com.noura.platform.commerce.api.v1.dto.common.ApiEnvelope;
import com.noura.platform.commerce.api.v1.support.ApiTrace;
import com.noura.platform.commerce.customers.domain.StorefrontCustomerPrincipal;
import com.noura.platform.dto.payment.PaymentTransactionResult;
import com.noura.platform.service.UnifiedPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Profile("legacy-storefront")
@RestController
@RequestMapping("/api/storefront/v1/orders/{orderId}/payments")
@Validated
public class StorefrontPaymentController {
    private final UnifiedPaymentService unifiedPaymentService;

    public StorefrontPaymentController(UnifiedPaymentService unifiedPaymentService) {
        this.unifiedPaymentService = unifiedPaymentService;
    }

    @GetMapping
    public ApiEnvelope<List<PaymentTransactionResult>> listPayments(@PathVariable Long orderId,
                                                                                           Authentication authentication,
                                                                                           HttpServletRequest requestContext) {
        Long customerId = resolveCustomerId(authentication);
        return ApiEnvelope.success(
                "STORE_PAYMENT_LIST_OK",
                "Payments fetched successfully.",
                unifiedPaymentService.listStorefrontPayments(customerId, orderId),
                ApiTrace.resolve(requestContext)
        );
    }

    @PostMapping
    public ApiEnvelope<PaymentTransactionResult> createPayment(@PathVariable Long orderId,
                                                                                      @Valid @RequestBody CreatePaymentRequest body,
                                                                                      Authentication authentication,
                                                                                      HttpServletRequest requestContext) {
        Long customerId = resolveCustomerId(authentication);
        var result = unifiedPaymentService.createStorefrontPayment(
                customerId,
                orderId,
                new com.noura.platform.dto.payment.CreatePaymentRequest(
                        body == null ? null : body.paymentMethod(),
                        body == null ? null : body.provider(),
                        body == null ? null : body.providerReference()
                )
        );
        return ApiEnvelope.success(
                "STORE_PAYMENT_CREATE_OK",
                "Payment created successfully.",
                result,
                ApiTrace.resolve(requestContext)
        );
    }

    @PostMapping("/{paymentId}/capture")
    public ApiEnvelope<PaymentTransactionResult> capture(@PathVariable Long orderId,
                                                                                @PathVariable Long paymentId,
                                                                                Authentication authentication,
                                                                                HttpServletRequest requestContext) {
        Long customerId = resolveCustomerId(authentication);
        return ApiEnvelope.success(
                "STORE_PAYMENT_CAPTURE_OK",
                "Payment captured successfully.",
                unifiedPaymentService.captureStorefrontPayment(customerId, orderId, paymentId),
                ApiTrace.resolve(requestContext)
        );
    }

    private Long resolveCustomerId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication required.");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof StorefrontCustomerPrincipal customerPrincipal)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Customer authentication required.");
        }
        if (customerPrincipal.id() == null || customerPrincipal.id() <= 0) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid customer identity.");
        }
        return customerPrincipal.id();
    }

    public record CreatePaymentRequest(
            @Size(max = 64, message = "provider length must be <= 64")
            String provider,
            @Size(max = 64, message = "paymentMethod length must be <= 64")
            String paymentMethod,
            @Size(max = 128, message = "providerReference length must be <= 128")
            String providerReference
    ) {
    }
}
