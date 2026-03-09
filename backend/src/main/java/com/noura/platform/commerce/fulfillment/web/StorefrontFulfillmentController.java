package com.noura.platform.commerce.fulfillment.web;

import com.noura.platform.commerce.api.v1.dto.common.ApiEnvelope;
import com.noura.platform.commerce.api.v1.support.ApiTrace;
import com.noura.platform.commerce.customers.domain.StorefrontCustomerPrincipal;
import com.noura.platform.commerce.fulfillment.application.StorefrontFulfillmentService;
import com.noura.platform.dto.fulfillment.ShipmentDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Profile("legacy-storefront")
@RestController
@RequestMapping("/api/storefront/v1/orders/{orderId}/fulfillment")
public class StorefrontFulfillmentController {
    private final StorefrontFulfillmentService fulfillmentService;

    public StorefrontFulfillmentController(StorefrontFulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @GetMapping
    public ApiEnvelope<ShipmentDto> getLatest(@PathVariable Long orderId,
                                                                          Authentication authentication,
                                                                          HttpServletRequest requestContext) {
        Long customerId = resolveCustomerId(authentication);
        return ApiEnvelope.success(
                "STORE_FULFILLMENT_GET_OK",
                "Fulfillment status fetched successfully.",
                fulfillmentService.getLatestForOrder(customerId, orderId),
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
}
