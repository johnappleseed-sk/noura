package com.noura.customer.controller;

import com.noura.customer.common.ApiResponse;
import com.noura.customer.controller.support.CustomerIdentityResolver;
import com.noura.customer.domain.enums.DefaultAddressType;
import com.noura.customer.dto.address.CustomerAddressRequest;
import com.noura.customer.dto.address.CustomerAddressResponse;
import com.noura.customer.dto.payment.CustomerPaymentMethodRequest;
import com.noura.customer.dto.payment.CustomerPaymentMethodResponse;
import com.noura.customer.dto.profile.CustomerProfileResponse;
import com.noura.customer.dto.profile.UpdateCustomerProfileRequest;
import com.noura.customer.service.CustomerAccountService;
import com.noura.customer.service.model.CustomerIdentity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Customer self-service account API for profile and address management.
 *
 * <p>The controller exposes both versioned and legacy paths for storefront compatibility.</p>
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/v1/account", "/api/account"})
public class CustomerAccountController {

    private final CustomerAccountService customerAccountService;
    private final CustomerIdentityResolver customerIdentityResolver;

    /**
     * Returns current customer profile, creating one lazily if it does not exist yet.
     *
     * @param request current HTTP request
     * @return profile response envelope
     */
    @GetMapping("/profile")
    public ApiResponse<CustomerProfileResponse> getCurrentProfile(HttpServletRequest request) {
        CustomerIdentity identity = resolveIdentity(request);
        CustomerProfileResponse profile = customerAccountService.getOrCreateCurrentProfile(identity);
        return ApiResponse.ok("Profile", profile, request.getRequestURI());
    }

    /**
     * Updates current customer profile.
     *
     * @param payload profile update payload
     * @param request current HTTP request
     * @return updated profile response envelope
     */
    @PutMapping("/profile")
    public ApiResponse<CustomerProfileResponse> updateCurrentProfile(
            @Valid @RequestBody UpdateCustomerProfileRequest payload,
            HttpServletRequest request
    ) {
        CustomerIdentity identity = resolveIdentity(request);
        CustomerProfileResponse profile = customerAccountService.updateCurrentProfile(identity, payload);
        return ApiResponse.ok("Profile updated", profile, request.getRequestURI());
    }

    /**
     * Lists current customer addresses.
     *
     * @param request current HTTP request
     * @return address list response envelope
     */
    @GetMapping("/addresses")
    public ApiResponse<List<CustomerAddressResponse>> listAddresses(HttpServletRequest request) {
        CustomerIdentity identity = resolveIdentity(request);
        List<CustomerAddressResponse> addresses = customerAccountService.listAddresses(identity);
        return ApiResponse.ok("Addresses", addresses, request.getRequestURI());
    }

    /**
     * Gets one address for current customer.
     *
     * @param addressId address identifier
     * @param request current HTTP request
     * @return address response envelope
     */
    @GetMapping("/addresses/{addressId}")
    public ApiResponse<CustomerAddressResponse> getAddress(
            @PathVariable UUID addressId,
            HttpServletRequest request
    ) {
        CustomerIdentity identity = resolveIdentity(request);
        CustomerAddressResponse address = customerAccountService.getAddress(identity, addressId);
        return ApiResponse.ok("Address", address, request.getRequestURI());
    }

    /**
     * Adds one address for current customer.
     *
     * @param payload address create payload
     * @param request current HTTP request
     * @return created address response envelope
     */
    @PostMapping("/addresses")
    public ApiResponse<CustomerAddressResponse> addAddress(
            @Valid @RequestBody CustomerAddressRequest payload,
            HttpServletRequest request
    ) {
        CustomerIdentity identity = resolveIdentity(request);
        CustomerAddressResponse address = customerAccountService.addAddress(identity, payload);
        return ApiResponse.ok("Address added", address, request.getRequestURI());
    }

    /**
     * Updates one address for current customer.
     *
     * @param addressId address identifier
     * @param payload address update payload
     * @param request current HTTP request
     * @return updated address response envelope
     */
    @PutMapping("/addresses/{addressId}")
    public ApiResponse<CustomerAddressResponse> updateAddress(
            @PathVariable UUID addressId,
            @Valid @RequestBody CustomerAddressRequest payload,
            HttpServletRequest request
    ) {
        CustomerIdentity identity = resolveIdentity(request);
        CustomerAddressResponse address = customerAccountService.updateAddress(identity, addressId, payload);
        return ApiResponse.ok("Address updated", address, request.getRequestURI());
    }

    /**
     * Deletes one address for current customer.
     *
     * @param addressId address identifier
     * @param request current HTTP request
     * @return empty success response envelope
     */
    @DeleteMapping("/addresses/{addressId}")
    public ApiResponse<Void> deleteAddress(
            @PathVariable UUID addressId,
            HttpServletRequest request
    ) {
        CustomerIdentity identity = resolveIdentity(request);
        customerAccountService.deleteAddress(identity, addressId);
        return ApiResponse.ok("Address deleted", null, request.getRequestURI());
    }

    /**
     * Sets default shipping/billing marker for one address.
     *
     * <p>When {@code type} is not provided, the endpoint defaults to {@link DefaultAddressType#BOTH}
     * for legacy storefront compatibility.</p>
     *
     * @param addressId address identifier
     * @param type default marker type
     * @param request current HTTP request
     * @return updated address response envelope
     */
    @PostMapping("/addresses/{addressId}/set-default")
    public ApiResponse<CustomerAddressResponse> setDefaultAddress(
            @PathVariable UUID addressId,
            @RequestParam(name = "type", required = false) DefaultAddressType type,
            HttpServletRequest request
    ) {
        CustomerIdentity identity = resolveIdentity(request);
        DefaultAddressType resolvedType = type == null ? DefaultAddressType.BOTH : type;
        CustomerAddressResponse address = customerAccountService.setDefaultAddress(identity, addressId, resolvedType);
        return ApiResponse.ok("Default address updated", address, request.getRequestURI());
    }

    /**
     * Lists current customer payment methods.
     *
     * @param request current HTTP request
     * @return payment method list response envelope
     */
    @GetMapping("/payment-methods")
    public ApiResponse<List<CustomerPaymentMethodResponse>> listPaymentMethods(HttpServletRequest request) {
        CustomerIdentity identity = resolveIdentity(request);
        List<CustomerPaymentMethodResponse> paymentMethods = customerAccountService.listPaymentMethods(identity);
        return ApiResponse.ok("Payment methods", paymentMethods, request.getRequestURI());
    }

    /**
     * Adds one payment method for current customer.
     *
     * @param payload payment method payload
     * @param request current HTTP request
     * @return created payment method response envelope
     */
    @PostMapping("/payment-methods")
    public ApiResponse<CustomerPaymentMethodResponse> addPaymentMethod(
            @Valid @RequestBody CustomerPaymentMethodRequest payload,
            HttpServletRequest request
    ) {
        CustomerIdentity identity = resolveIdentity(request);
        CustomerPaymentMethodResponse paymentMethod = customerAccountService.addPaymentMethod(identity, payload);
        return ApiResponse.ok("Payment method added", paymentMethod, request.getRequestURI());
    }

    /**
     * Updates one payment method for current customer.
     *
     * @param paymentMethodId payment method identifier
     * @param payload payment method payload
     * @param request current HTTP request
     * @return updated payment method response envelope
     */
    @PutMapping("/payment-methods/{paymentMethodId}")
    public ApiResponse<CustomerPaymentMethodResponse> updatePaymentMethod(
            @PathVariable UUID paymentMethodId,
            @Valid @RequestBody CustomerPaymentMethodRequest payload,
            HttpServletRequest request
    ) {
        CustomerIdentity identity = resolveIdentity(request);
        CustomerPaymentMethodResponse paymentMethod =
                customerAccountService.updatePaymentMethod(identity, paymentMethodId, payload);
        return ApiResponse.ok("Payment method updated", paymentMethod, request.getRequestURI());
    }

    /**
     * Deletes one payment method for current customer.
     *
     * @param paymentMethodId payment method identifier
     * @param request current HTTP request
     * @return empty success response envelope
     */
    @DeleteMapping("/payment-methods/{paymentMethodId}")
    public ApiResponse<Void> deletePaymentMethod(
            @PathVariable UUID paymentMethodId,
            HttpServletRequest request
    ) {
        CustomerIdentity identity = resolveIdentity(request);
        customerAccountService.deletePaymentMethod(identity, paymentMethodId);
        return ApiResponse.ok("Payment method deleted", null, request.getRequestURI());
    }

    /**
     * Resolves required customer identity for current request.
     *
     * @param request current HTTP request
     * @return resolved customer identity context
     */
    private CustomerIdentity resolveIdentity(HttpServletRequest request) {
        return customerIdentityResolver.resolveRequiredIdentity(request);
    }
}
