package com.noura.platform.service.impl;

import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.domain.entity.*;
import com.noura.platform.dto.order.OrderDto;
import com.noura.platform.dto.order.OrderItemDto;
import com.noura.platform.dto.user.*;
import com.noura.platform.mapper.*;
import com.noura.platform.repository.*;
import com.noura.platform.security.SecurityUtils;
import com.noura.platform.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final AddressRepository addressRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final B2BCompanyProfileRepository companyProfileRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final PaymentMethodMapper paymentMethodMapper;
    private final CompanyMapper companyMapper;
    private final ApprovalMapper approvalMapper;
    private final OrderMapper orderMapper;

    /**
     * Retrieves my profile.
     *
     * @return The mapped DTO representation.
     */
    @Override
    public UserProfileDto getMyProfile() {
        return userMapper.toDto(currentUser());
    }

    /**
     * Updates my profile.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    public UserProfileDto updateMyProfile(UpdateProfileRequest request) {
        UserAccount user = currentUser();
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        return userMapper.toDto(userAccountRepository.save(user));
    }

    /**
     * Lists addresses.
     *
     * @return A list of matching items.
     */
    @Override
    public List<AddressDto> listAddresses() {
        return addressRepository.findByUser(currentUser()).stream().map(addressMapper::toDto).toList();
    }

    /**
     * Adds address.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    public AddressDto addAddress(AddressRequest request) {
        UserAccount user = currentUser();
        if (request.defaultAddress()) {
            addressRepository.findByUser(user).forEach(address -> {
                address.setDefaultAddress(false);
                addressRepository.save(address);
            });
        }
        Address entity = new Address();
        entity.setUser(user);
        applyAddressFields(entity, request);
        return addressMapper.toDto(addressRepository.save(entity));
    }

    /**
     * Updates address.
     *
     * @param addressId The address id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    public AddressDto updateAddress(UUID addressId, AddressRequest request) {
        UserAccount user = currentUser();
        Address address = addressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new NotFoundException("ADDRESS_NOT_FOUND", "Address not found"));
        if (request.defaultAddress()) {
            addressRepository.findByUser(user).forEach(item -> {
                item.setDefaultAddress(false);
                addressRepository.save(item);
            });
        }
        applyAddressFields(address, request);
        return addressMapper.toDto(addressRepository.save(address));
    }

    /**
     * Deletes address.
     *
     * @param addressId The address id used to locate the target record.
     */
    @Override
    @Transactional
    public void deleteAddress(UUID addressId) {
        UserAccount user = currentUser();
        Address address = addressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new NotFoundException("ADDRESS_NOT_FOUND", "Address not found"));
        addressRepository.delete(address);
    }

    /**
     * Lists payment methods.
     *
     * @return A list of matching items.
     */
    @Override
    public List<PaymentMethodDto> listPaymentMethods() {
        return paymentMethodRepository.findByUser(currentUser()).stream().map(paymentMethodMapper::toDto).toList();
    }

    /**
     * Adds payment method.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    public PaymentMethodDto addPaymentMethod(PaymentMethodRequest request) {
        UserAccount user = currentUser();
        if (request.defaultMethod()) {
            paymentMethodRepository.findByUser(user).forEach(method -> {
                method.setDefaultMethod(false);
                paymentMethodRepository.save(method);
            });
        }
        PaymentMethod method = new PaymentMethod();
        method.setUser(user);
        method.setMethodType(request.methodType());
        method.setProvider(request.provider());
        method.setTokenizedReference(request.tokenizedReference());
        method.setDefaultMethod(request.defaultMethod());
        return paymentMethodMapper.toDto(paymentMethodRepository.save(method));
    }

    /**
     * Updates payment method.
     *
     * @param paymentMethodId The payment method id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    public PaymentMethodDto updatePaymentMethod(UUID paymentMethodId, PaymentMethodRequest request) {
        UserAccount user = currentUser();
        PaymentMethod method = paymentMethodRepository.findByIdAndUser(paymentMethodId, user)
                .orElseThrow(() -> new NotFoundException("PAYMENT_METHOD_NOT_FOUND", "Payment method not found"));
        if (request.defaultMethod()) {
            paymentMethodRepository.findByUser(user).forEach(item -> {
                item.setDefaultMethod(false);
                paymentMethodRepository.save(item);
            });
        }
        method.setMethodType(request.methodType());
        method.setProvider(request.provider());
        method.setTokenizedReference(request.tokenizedReference());
        method.setDefaultMethod(request.defaultMethod());
        return paymentMethodMapper.toDto(paymentMethodRepository.save(method));
    }

    /**
     * Deletes payment method.
     *
     * @param paymentMethodId The payment method id used to locate the target record.
     */
    @Override
    @Transactional
    public void deletePaymentMethod(UUID paymentMethodId) {
        UserAccount user = currentUser();
        PaymentMethod method = paymentMethodRepository.findByIdAndUser(paymentMethodId, user)
                .orElseThrow(() -> new NotFoundException("PAYMENT_METHOD_NOT_FOUND", "Payment method not found"));
        paymentMethodRepository.delete(method);
    }

    /**
     * Retrieves my order history.
     *
     * @return A list of matching items.
     */
    @Override
    public List<OrderDto> myOrderHistory() {
        return orderRepository.findTop10ByUserOrderByCreatedAtDesc(currentUser())
                .stream()
                .map(this::mapOrderWithItems)
                .toList();
    }

    /**
     * Executes quick reorder.
     *
     * @param orderId The order id used to locate the target record.
     * @return A list of matching items.
     */
    @Override
    @Transactional
    public List<OrderDto> quickReorder(UUID orderId) {
        UserAccount user = currentUser();
        Order order = orderRepository.findById(orderId)
                .filter(item -> item.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        Cart cart = cartRepository.findByUser(user).orElseGet(() -> {
            Cart created = new Cart();
            created.setUser(user);
            created.setStore(order.getStore());
            return cartRepository.save(created);
        });
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        for (OrderItem source : orderItems) {
            CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), source.getProduct().getId())
                    .orElseGet(() -> {
                        CartItem item = new CartItem();
                        item.setCart(cart);
                        item.setProduct(source.getProduct());
                        item.setQuantity(0);
                        item.setUnitPrice(source.getUnitPrice());
                        return item;
                    });
            cartItem.setQuantity(cartItem.getQuantity() + source.getQuantity());
            cartItemRepository.save(cartItem);
        }
        return myOrderHistory();
    }

    /**
     * Executes upsert company profile.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('B2B','ADMIN')")
    public CompanyProfileDto upsertCompanyProfile(CompanyProfileRequest request) {
        UserAccount user = currentUser();
        B2BCompanyProfile profile = companyProfileRepository.findByUser(user).orElseGet(() -> {
            B2BCompanyProfile entity = new B2BCompanyProfile();
            entity.setUser(user);
            return entity;
        });
        profile.setCompanyName(request.companyName());
        profile.setTaxId(request.taxId());
        profile.setCostCenter(request.costCenter());
        profile.setApprovalEmail(request.approvalEmail());
        profile.setApprovalRequired(request.approvalRequired());
        profile.setApprovalThreshold(request.approvalThreshold());
        return companyMapper.toDto(companyProfileRepository.save(profile));
    }

    /**
     * Retrieves my approvals.
     *
     * @return A list of matching items.
     */
    @Override
    public List<ApprovalDto> myApprovals() {
        return approvalRequestRepository.findByRequester(currentUser()).stream().map(approvalMapper::toDto).toList();
    }

    /**
     * Lists users.
     *
     * @param pageable The pagination configuration.
     * @return A paginated result set.
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserProfileDto> listUsers(Pageable pageable) {
        return userAccountRepository.findAll(pageable).map(userMapper::toDto);
    }

    /**
     * Executes admin update user.
     *
     * @param userId The user id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserProfileDto adminUpdateUser(UUID userId, AdminUserUpdateRequest request) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        if (request.roles() != null && !request.roles().isEmpty()) {
            user.setRoles(request.roles());
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        return userMapper.toDto(userAccountRepository.save(user));
    }

    /**
     * Deletes my account.
     */
    @Override
    @Transactional
    public void deleteMyAccount() {
        UserAccount user = currentUser();
        userAccountRepository.delete(user);
    }

    /**
     * Executes current user.
     *
     * @return The result of current user.
     */
    private UserAccount currentUser() {
        return userAccountRepository.findByEmailIgnoreCase(SecurityUtils.currentEmail())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Authenticated user not found"));
    }

    /**
     * Applies address fields with enterprise validation.
     * <p>
     * Coordinates are optional, but latitude/longitude must be provided together when present.
     * This prevents partially-specified locations from being persisted and later misused for service-area logic.
     * </p>
     */
    private void applyAddressFields(Address entity, AddressRequest request) {
        boolean hasLat = request.latitude() != null;
        boolean hasLng = request.longitude() != null;
        if (hasLat != hasLng) {
            throw new BadRequestException("ADDRESS_COORDINATES_INVALID", "Latitude and longitude must be provided together.");
        }

        entity.setLabel(request.label());
        entity.setFullName(request.fullName());
        entity.setPhone(request.phone());
        entity.setLine1(request.line1());
        entity.setLine2(request.line2());
        entity.setDistrict(request.district());
        entity.setCity(request.city());
        entity.setState(request.state());
        entity.setZipCode(request.zipCode());
        entity.setCountry(request.country());
        entity.setLatitude(request.latitude());
        entity.setLongitude(request.longitude());
        entity.setAccuracyMeters(request.accuracyMeters());
        entity.setPlaceId(request.placeId());
        entity.setFormattedAddress(request.formattedAddress());
        entity.setDeliveryInstructions(request.deliveryInstructions());
        entity.setDefaultAddress(request.defaultAddress());
    }

    /**
     * Transforms data for order with items.
     *
     * @param order The order value.
     * @return The mapped DTO representation.
     */
    private OrderDto mapOrderWithItems(Order order) {
        List<OrderItemDto> items = orderItemRepository.findByOrderId(order.getId())
                .stream()
                .map(orderMapper::toItemDto)
                .toList();
        OrderDto dto = orderMapper.toDto(order);
        return new OrderDto(
                dto.id(),
                dto.userId(),
                dto.storeId(),
                dto.subtotal(),
                dto.discountAmount(),
                dto.shippingAmount(),
                dto.totalAmount(),
                dto.fulfillmentMethod(),
                dto.status(),
                dto.refundStatus(),
                dto.couponCode(),
                dto.createdAt(),
                items
        );
    }
}
