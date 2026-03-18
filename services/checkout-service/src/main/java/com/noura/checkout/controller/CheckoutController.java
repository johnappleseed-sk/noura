package com.noura.checkout.controller;

import com.noura.checkout.common.ApiResponse;
import com.noura.checkout.controller.support.CheckoutRequestContextResolver;
import com.noura.checkout.dto.checkout.CheckoutPlaceOrderRequest;
import com.noura.checkout.dto.checkout.CheckoutPlaceOrderResponse;
import com.noura.checkout.dto.checkout.CheckoutPreviewRequest;
import com.noura.checkout.dto.checkout.CheckoutPreviewResponse;
import com.noura.checkout.dto.checkout.CheckoutValidateRequest;
import com.noura.checkout.dto.checkout.CheckoutValidationResponse;
import com.noura.checkout.dto.checkout.LegacyCheckoutRequest;
import com.noura.checkout.service.CheckoutOrchestrationService;
import com.noura.checkout.service.model.CheckoutRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Checkout orchestration controller for preview, validation, and place-order APIs.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping
public class CheckoutController {

    private final CheckoutOrchestrationService checkoutOrchestrationService;
    private final CheckoutRequestContextResolver contextResolver;

    /**
     * Returns checkout preview snapshot.
     *
     * @param requestBody preview request payload
     * @param request current HTTP request
     * @return checkout preview response envelope
     */
    @PostMapping({"/api/v1/checkout/preview", "/api/checkout/preview"})
    public ApiResponse<CheckoutPreviewResponse> preview(
            @RequestBody(required = false) @Valid CheckoutPreviewRequest requestBody,
            HttpServletRequest request
    ) {
        CheckoutRequestContext context = contextResolver.resolve(request);
        CheckoutPreviewResponse data = checkoutOrchestrationService.preview(context, requestBody);
        return ApiResponse.ok("Checkout preview", data, request.getRequestURI());
    }

    /**
     * Returns checkout validation snapshot.
     *
     * @param requestBody validation request payload
     * @param request current HTTP request
     * @return checkout validation response envelope
     */
    @PostMapping({"/api/v1/checkout/validate", "/api/checkout/validate"})
    public ApiResponse<CheckoutValidationResponse> validate(
            @RequestBody(required = false) @Valid CheckoutValidateRequest requestBody,
            HttpServletRequest request
    ) {
        CheckoutRequestContext context = contextResolver.resolve(request);
        CheckoutValidationResponse data = checkoutOrchestrationService.validate(context, requestBody);
        return ApiResponse.ok("Checkout validation", data, request.getRequestURI());
    }

    /**
     * Places an order from validated checkout data.
     *
     * @param requestBody place-order request payload
     * @param idempotencyKey optional idempotency header value
     * @param request current HTTP request
     * @return place-order response envelope
     */
    @PostMapping({"/api/v1/checkout/place-order", "/api/checkout/place-order"})
    public ResponseEntity<ApiResponse<CheckoutPlaceOrderResponse>> placeOrder(
            @RequestBody(required = false) @Valid CheckoutPlaceOrderRequest requestBody,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest request
    ) {
        CheckoutRequestContext context = contextResolver.resolve(request);
        CheckoutPlaceOrderResponse data = checkoutOrchestrationService.placeOrder(context, requestBody, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order placed", data, request.getRequestURI()));
    }

    /**
     * Compatibility direct-checkout endpoint used by storefront legacy path.
     *
     * @param requestBody legacy checkout payload
     * @param idempotencyKey optional idempotency header value
     * @param request current HTTP request
     * @return place-order response envelope
     */
    @PostMapping({"/api/v1/checkout", "/api/checkout"})
    public ResponseEntity<ApiResponse<CheckoutPlaceOrderResponse>> directCheckout(
            @RequestBody(required = false) @Valid LegacyCheckoutRequest requestBody,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest request
    ) {
        CheckoutRequestContext context = contextResolver.resolve(request);
        CheckoutPlaceOrderRequest placeOrderRequest = requestBody == null
                ? new CheckoutPlaceOrderRequest(null, null, null, null, null, null, null, null)
                : new CheckoutPlaceOrderRequest(
                requestBody.storeId(),
                requestBody.addressId(),
                requestBody.couponCode(),
                requestBody.paymentMethod(),
                requestBody.paymentProvider(),
                requestBody.paymentProviderReference(),
                requestBody.paymentAutoCapture(),
                requestBody.idempotencyKey()
        );
        CheckoutPlaceOrderResponse data = checkoutOrchestrationService.placeOrder(context, placeOrderRequest, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order placed", data, request.getRequestURI()));
    }
}
