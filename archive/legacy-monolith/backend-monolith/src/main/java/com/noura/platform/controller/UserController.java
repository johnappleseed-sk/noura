package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.order.OrderDto;
import com.noura.platform.dto.user.*;
import com.noura.platform.service.UnifiedOrderService;
import com.noura.platform.service.UnifiedPaymentService;
import com.noura.platform.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/account")
public class UserController {

    private final UserAccountService userAccountService;
    private final UnifiedOrderService unifiedOrderService;
    private final UnifiedPaymentService unifiedPaymentService;

    /**
     * Executes profile.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @GetMapping("/profile")
    public ApiResponse<UserProfileDto> profile(HttpServletRequest http) {
        return ApiResponse.ok("Profile", userAccountService.getMyProfile(), http.getRequestURI());
    }

    /**
     * Updates profile.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PutMapping("/profile")
    public ApiResponse<UserProfileDto> updateProfile(@Valid @RequestBody UpdateProfileRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Profile updated", userAccountService.updateMyProfile(request), http.getRequestURI());
    }

    /**
     * Adds addresses.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("/addresses")
    public ApiResponse<List<AddressDto>> addresses(HttpServletRequest http) {
        return ApiResponse.ok("Addresses", userAccountService.listAddresses(), http.getRequestURI());
    }

    @GetMapping("/addresses/{addressId}")
    public ApiResponse<AddressDto> address(@PathVariable UUID addressId, HttpServletRequest http) {
        return ApiResponse.ok("Address", userAccountService.getAddress(addressId), http.getRequestURI());
    }

    /**
     * Adds address.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/addresses")
    public ApiResponse<AddressDto> addAddress(@Valid @RequestBody AddressRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Address added", userAccountService.addAddress(request), http.getRequestURI());
    }

    /**
     * Updates address.
     *
     * @param addressId The address id used to locate the target record.
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PutMapping("/addresses/{addressId}")
    public ApiResponse<AddressDto> updateAddress(
            @PathVariable UUID addressId,
            @Valid @RequestBody AddressRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Address updated", userAccountService.updateAddress(addressId, request), http.getRequestURI());
    }

    /**
     * Deletes address.
     *
     * @param addressId The address id used to locate the target record.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @DeleteMapping("/addresses/{addressId}")
    public ApiResponse<Void> deleteAddress(@PathVariable UUID addressId, HttpServletRequest http) {
        userAccountService.deleteAddress(addressId);
        return ApiResponse.ok("Address deleted", null, http.getRequestURI());
    }

    @PostMapping("/addresses/{addressId}/set-default")
    public ApiResponse<AddressDto> setDefaultAddress(@PathVariable UUID addressId, HttpServletRequest http) {
        return ApiResponse.ok("Default address updated", userAccountService.setDefaultAddress(addressId), http.getRequestURI());
    }

    /**
     * Executes payment methods.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("/payment-methods")
    public ApiResponse<List<PaymentMethodDto>> paymentMethods(HttpServletRequest http) {
        return ApiResponse.ok("Payment methods", unifiedPaymentService.listPaymentMethods(), http.getRequestURI());
    }

    /**
     * Adds payment method.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/payment-methods")
    public ApiResponse<PaymentMethodDto> addPaymentMethod(@Valid @RequestBody PaymentMethodRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Payment method added", unifiedPaymentService.addPaymentMethod(request), http.getRequestURI());
    }

    /**
     * Updates payment method.
     *
     * @param paymentMethodId The payment method id used to locate the target record.
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PutMapping("/payment-methods/{paymentMethodId}")
    public ApiResponse<PaymentMethodDto> updatePaymentMethod(
            @PathVariable UUID paymentMethodId,
            @Valid @RequestBody PaymentMethodRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Payment method updated",
                unifiedPaymentService.updatePaymentMethod(paymentMethodId, request),
                http.getRequestURI()
        );
    }

    /**
     * Deletes payment method.
     *
     * @param paymentMethodId The payment method id used to locate the target record.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @DeleteMapping("/payment-methods/{paymentMethodId}")
    public ApiResponse<Void> deletePaymentMethod(@PathVariable UUID paymentMethodId, HttpServletRequest http) {
        unifiedPaymentService.deletePaymentMethod(paymentMethodId);
        return ApiResponse.ok("Payment method deleted", null, http.getRequestURI());
    }

    /**
     * Executes order history.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("/orders")
    public ApiResponse<List<OrderDto>> orderHistory(HttpServletRequest http) {
        return ApiResponse.ok("Order history", unifiedOrderService.myOrderHistory(), http.getRequestURI());
    }

    /**
     * Executes quick reorder.
     *
     * @param orderId The order id used to locate the target record.
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @PostMapping("/orders/{orderId}/quick-reorder")
    public ApiResponse<List<OrderDto>> quickReorder(@PathVariable UUID orderId, HttpServletRequest http) {
        return ApiResponse.ok("Quick reorder prepared", unifiedOrderService.quickReorder(orderId), http.getRequestURI());
    }

    /**
     * Executes upsert company profile.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PutMapping("/company-profile")
    public ApiResponse<CompanyProfileDto> upsertCompanyProfile(
            @Valid @RequestBody CompanyProfileRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Company profile saved", userAccountService.upsertCompanyProfile(request), http.getRequestURI());
    }

    /**
     * Retrieves my approvals.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("/approvals")
    public ApiResponse<List<ApprovalDto>> myApprovals(HttpServletRequest http) {
        return ApiResponse.ok("Approvals", userAccountService.myApprovals(), http.getRequestURI());
    }

    /**
     * Deletes account.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @DeleteMapping("/profile")
    public ApiResponse<Void> deleteAccount(HttpServletRequest http) {
        userAccountService.deleteMyAccount();
        return ApiResponse.ok("Account deleted", null, http.getRequestURI());
    }
}
