package com.noura.platform.commerce.customers.web;

import com.noura.platform.commerce.api.v1.dto.common.ApiEnvelope;
import com.noura.platform.commerce.api.v1.support.ApiTrace;
import com.noura.platform.commerce.customers.application.StorefrontCustomerAddressService;
import com.noura.platform.commerce.customers.domain.StorefrontCustomerPrincipal;
import com.noura.platform.dto.storefront.CustomerAddressDto;
import com.noura.platform.dto.storefront.StorefrontCustomerAddressRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Profile("legacy-storefront")
@RestController
@RequestMapping("/api/storefront/v1/customers/me/addresses")
@Validated
public class StorefrontCustomerAddressController {
    private final StorefrontCustomerAddressService customerAddressService;

    public StorefrontCustomerAddressController(StorefrontCustomerAddressService customerAddressService) {
        this.customerAddressService = customerAddressService;
    }

    @GetMapping
    public ApiEnvelope<List<CustomerAddressDto>> list(Authentication authentication,
                                                                                   HttpServletRequest requestContext) {
        Long customerId = resolveCustomerId(authentication);
        return ApiEnvelope.success(
                "CUSTOMER_ADDRESSES_LIST_OK",
                "Customer addresses fetched successfully.",
                customerAddressService.listAddresses(customerId),
                ApiTrace.resolve(requestContext)
        );
    }

    @PostMapping
    public ApiEnvelope<CustomerAddressDto> create(Authentication authentication,
                                                                                 @Valid @RequestBody CreateAddressRequest request,
                                                                                 HttpServletRequest requestContext) {
        Long customerId = resolveCustomerId(authentication);
        StorefrontCustomerAddressRequest serviceRequest =
                new StorefrontCustomerAddressRequest(
                        request.label(),
                        request.recipientName(),
                        request.phone(),
                        request.line1(),
                        request.line2(),
                        request.district(),
                        request.city(),
                        request.stateProvince(),
                        request.postalCode(),
                        request.countryCode(),
                        Boolean.TRUE.equals(request.defaultShipping()),
                        Boolean.TRUE.equals(request.defaultBilling())
                );
        return ApiEnvelope.success(
                "CUSTOMER_ADDRESSES_CREATE_OK",
                "Customer address created successfully.",
                customerAddressService.addAddress(customerId, serviceRequest),
                ApiTrace.resolve(requestContext)
        );
    }

    @DeleteMapping("/{addressId}")
    public ApiEnvelope<Void> delete(Authentication authentication,
                                   @PathVariable Long addressId,
                                   HttpServletRequest requestContext) {
        Long customerId = resolveCustomerId(authentication);
        customerAddressService.deleteAddress(customerId, addressId);
        return ApiEnvelope.success(
                "CUSTOMER_ADDRESSES_DELETE_OK",
                "Customer address deleted successfully.",
                null,
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

    public record CreateAddressRequest(
            String label,
            @NotBlank(message = "recipientName is required.")
            @Size(max = 120, message = "recipientName length must be <= 120")
            String recipientName,

            String phone,

            @NotBlank(message = "line1 is required.")
            @Size(max = 255, message = "line1 length must be <= 255")
            String line1,

            String line2,

            String district,

            @NotBlank(message = "city is required.")
            @Size(max = 120, message = "city length must be <= 120")
            String city,

            String stateProvince,

            String postalCode,

            @NotBlank(message = "countryCode is required.")
            @Size(min = 2, max = 2, message = "countryCode must be exactly 2 characters")
            String countryCode,

            Boolean defaultShipping,
            Boolean defaultBilling
    ) {
    }
}
