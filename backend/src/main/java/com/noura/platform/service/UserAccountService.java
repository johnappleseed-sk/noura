package com.noura.platform.service;

import com.noura.platform.dto.order.OrderDto;
import com.noura.platform.dto.user.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface UserAccountService {
    /**
     * Retrieves my profile.
     *
     * @return The mapped DTO representation.
     */
    UserProfileDto getMyProfile();

    /**
     * Updates my profile.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    UserProfileDto updateMyProfile(UpdateProfileRequest request);

    /**
     * Lists addresses.
     *
     * @return A list of matching items.
     */
    List<AddressDto> listAddresses();

    /**
     * Adds address.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    AddressDto addAddress(AddressRequest request);

    /**
     * Updates address.
     *
     * @param addressId The address id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    AddressDto updateAddress(UUID addressId, AddressRequest request);

    /**
     * Deletes address.
     *
     * @param addressId The address id used to locate the target record.
     */
    void deleteAddress(UUID addressId);

    /**
     * Lists payment methods.
     *
     * @return A list of matching items.
     */
    List<PaymentMethodDto> listPaymentMethods();

    /**
     * Adds payment method.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    PaymentMethodDto addPaymentMethod(PaymentMethodRequest request);

    /**
     * Updates payment method.
     *
     * @param paymentMethodId The payment method id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    PaymentMethodDto updatePaymentMethod(UUID paymentMethodId, PaymentMethodRequest request);

    /**
     * Deletes payment method.
     *
     * @param paymentMethodId The payment method id used to locate the target record.
     */
    void deletePaymentMethod(UUID paymentMethodId);

    /**
     * Retrieves my order history.
     *
     * @return A list of matching items.
     */
    List<OrderDto> myOrderHistory();

    /**
     * Executes quick reorder.
     *
     * @param orderId The order id used to locate the target record.
     * @return A list of matching items.
     */
    List<OrderDto> quickReorder(UUID orderId);

    /**
     * Executes upsert company profile.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    CompanyProfileDto upsertCompanyProfile(CompanyProfileRequest request);

    /**
     * Retrieves my approvals.
     *
     * @return A list of matching items.
     */
    List<ApprovalDto> myApprovals();

    /**
     * Lists users.
     *
     * @param pageable The pagination configuration.
     * @return A paginated result set.
     */
    Page<UserProfileDto> listUsers(Pageable pageable);

    /**
     * Executes admin update user.
     *
     * @param userId The user id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    UserProfileDto adminUpdateUser(UUID userId, AdminUserUpdateRequest request);

    /**
     * Deletes my account.
     */
    void deleteMyAccount();
}
