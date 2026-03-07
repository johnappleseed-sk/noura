package com.noura.platform.commerce.orders.web;

import com.noura.platform.commerce.api.v1.dto.common.ApiEnvelope;
import com.noura.platform.commerce.api.v1.support.ApiTrace;
import com.noura.platform.commerce.customers.domain.StorefrontCustomerPrincipal;
import com.noura.platform.commerce.orders.application.StorefrontOrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/storefront/v1/orders")
@Validated
public class StorefrontOrderController {
    private final StorefrontOrderService storefrontOrderService;

    public StorefrontOrderController(StorefrontOrderService storefrontOrderService) {
        this.storefrontOrderService = storefrontOrderService;
    }

    @PostMapping("/checkout")
    public ApiEnvelope<StorefrontOrderService.StorefrontOrderResult> checkout(@RequestBody CheckoutRequest body,
                                                                           Authentication authentication,
                                                                           HttpServletRequest request) {
        Long customerId = resolveCustomerId(authentication);
        var result = storefrontOrderService.checkout(customerId, new StorefrontOrderService.CheckoutRequest(
                body == null ? null : body.shippingAddressId(),
                body == null ? null : body.currency(),
                body == null ? null : body.paymentMethod(),
                body == null ? null : body.paymentProvider(),
                body == null ? null : body.paymentProviderReference()
        ));
        return ApiEnvelope.success(
                "STORE_ORDER_CHECKOUT_OK",
                "Order checkout completed successfully.",
                result,
                ApiTrace.resolve(request)
        );
    }

    @GetMapping("/me")
    public ApiEnvelope<List<StorefrontOrderService.OrderSummaryDto>> myOrders(Authentication authentication,
                                                                            HttpServletRequest request) {
        Long customerId = resolveCustomerId(authentication);
        return ApiEnvelope.success(
                "STORE_ORDER_LIST_OK",
                "Orders fetched successfully.",
                storefrontOrderService.listOrders(customerId),
                ApiTrace.resolve(request)
        );
    }

    @GetMapping("/{orderId}")
    public ApiEnvelope<StorefrontOrderService.StorefrontOrderResult> myOrder(Authentication authentication,
                                                                            @PathVariable Long orderId,
                                                                            HttpServletRequest request) {
        Long customerId = resolveCustomerId(authentication);
        return ApiEnvelope.success(
                "STORE_ORDER_DETAIL_OK",
                "Order detail fetched successfully.",
                storefrontOrderService.getOrder(customerId, orderId),
                ApiTrace.resolve(request)
        );
    }

    @PostMapping("/{orderId}/cancel")
    public ApiEnvelope<StorefrontOrderService.StorefrontOrderResult> cancel(@PathVariable Long orderId,
                                                                            Authentication authentication,
                                                                            HttpServletRequest request) {
        Long customerId = resolveCustomerId(authentication);
        return ApiEnvelope.success(
                "STORE_ORDER_CANCEL_OK",
                "Order cancelled successfully.",
                storefrontOrderService.cancel(customerId, orderId),
                ApiTrace.resolve(request)
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

    public record CheckoutRequest(Long shippingAddressId,
                                 @Size(max = 3, message = "currency length must be <= 3") String currency,
                                 String paymentMethod,
                                 String paymentProvider,
                                 String paymentProviderReference) {
    }
}
