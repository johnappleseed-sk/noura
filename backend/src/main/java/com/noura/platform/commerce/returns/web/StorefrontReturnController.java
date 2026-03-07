package com.noura.platform.commerce.returns.web;

import com.noura.platform.commerce.api.v1.dto.common.ApiEnvelope;
import com.noura.platform.commerce.api.v1.support.ApiTrace;
import com.noura.platform.commerce.customers.domain.StorefrontCustomerPrincipal;
import com.noura.platform.commerce.returns.application.ReturnService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Storefront API controller for customer return requests.
 */
@RestController
@RequestMapping("/api/storefront/v1/returns")
@Validated
public class StorefrontReturnController {
    private final ReturnService returnService;

    public StorefrontReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    @PostMapping
    public ApiEnvelope<ReturnService.ReturnRequestDto> createReturn(@Valid @RequestBody CreateReturnRequest body,
                                                                   Authentication authentication,
                                                                   HttpServletRequest request) {
        Long customerId = resolveCustomerId(authentication);
        var result = returnService.createReturnRequest(customerId, new ReturnService.CreateReturnRequest(
                body.orderId(),
                body.reason(),
                body.reasonDetails(),
                body.customerNotes(),
                body.items().stream()
                        .map(i -> new ReturnService.CreateReturnItemRequest(i.orderItemId(), i.quantity()))
                        .toList()
        ));
        return ApiEnvelope.success(
                "STOREFRONT_RETURN_CREATE_OK",
                "Return request created successfully.",
                result,
                ApiTrace.resolve(request)
        );
    }

    @GetMapping
    public ApiEnvelope<List<ReturnService.ReturnRequestDto>> listReturns(Authentication authentication,
                                                                        HttpServletRequest request) {
        Long customerId = resolveCustomerId(authentication);
        return ApiEnvelope.success(
                "STOREFRONT_RETURN_LIST_OK",
                "Returns fetched successfully.",
                returnService.listReturnRequests(customerId),
                ApiTrace.resolve(request)
        );
    }

    @GetMapping("/{returnId}")
    public ApiEnvelope<ReturnService.ReturnRequestDto> getReturn(@PathVariable Long returnId,
                                                                Authentication authentication,
                                                                HttpServletRequest request) {
        Long customerId = resolveCustomerId(authentication);
        return ApiEnvelope.success(
                "STOREFRONT_RETURN_FETCH_OK",
                "Return request fetched successfully.",
                returnService.getReturnRequest(customerId, returnId),
                ApiTrace.resolve(request)
        );
    }

    @PostMapping("/{returnId}/cancel")
    public ApiEnvelope<ReturnService.ReturnRequestDto> cancelReturn(@PathVariable Long returnId,
                                                                   Authentication authentication,
                                                                   HttpServletRequest request) {
        Long customerId = resolveCustomerId(authentication);
        return ApiEnvelope.success(
                "STOREFRONT_RETURN_CANCEL_OK",
                "Return request cancelled successfully.",
                returnService.cancelReturnRequest(customerId, returnId),
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

    // Request DTOs

    public record CreateReturnRequest(
            @NotNull(message = "orderId is required")
            Long orderId,

            @Size(max = 32, message = "reason length must be <= 32")
            String reason,

            @Size(max = 1000, message = "reasonDetails length must be <= 1000")
            String reasonDetails,

            @Size(max = 1000, message = "customerNotes length must be <= 1000")
            String customerNotes,

            @NotEmpty(message = "items is required")
            List<CreateReturnItemRequest> items
    ) {}

    public record CreateReturnItemRequest(
            @NotNull(message = "orderItemId is required")
            Long orderItemId,

            Integer quantity
    ) {}
}
