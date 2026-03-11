package com.noura.platform.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.common.exception.UnauthorizedException;
import com.noura.platform.config.AppProperties;
import com.noura.platform.domain.enums.AnalyticsEventType;
import com.noura.platform.domain.entity.ApprovalRequest;
import com.noura.platform.domain.entity.Address;
import com.noura.platform.domain.entity.Cart;
import com.noura.platform.domain.entity.CartItem;
import com.noura.platform.domain.entity.Order;
import com.noura.platform.domain.entity.OrderItem;
import com.noura.platform.domain.entity.OrderTimelineEvent;
import com.noura.platform.domain.entity.ProductInventory;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.enums.AddressValidationStatus;
import com.noura.platform.domain.enums.ApprovalStatus;
import com.noura.platform.domain.enums.StoreServiceType;
import com.noura.platform.domain.enums.OrderStatus;
import com.noura.platform.dto.cart.CartTotalsDto;
import com.noura.platform.dto.analytics.AnalyticsEventRequest;
import com.noura.platform.domain.enums.FulfillmentMethod;
import com.noura.platform.dto.location.ServiceAreaValidationRequest;
import com.noura.platform.dto.location.ServiceEligibilityDto;
import com.noura.platform.dto.order.CheckoutConfirmRequest;
import com.noura.platform.dto.order.CheckoutRequest;
import com.noura.platform.dto.order.OrderDto;
import com.noura.platform.dto.order.OrderItemDto;
import com.noura.platform.event.OrderCreatedEvent;
import com.noura.platform.mapper.OrderMapper;
import com.noura.platform.repository.ApprovalRequestRepository;
import com.noura.platform.repository.AddressRepository;
import com.noura.platform.repository.B2BCompanyProfileRepository;
import com.noura.platform.repository.CartItemRepository;
import com.noura.platform.repository.CartRepository;
import com.noura.platform.repository.OrderItemRepository;
import com.noura.platform.repository.OrderRepository;
import com.noura.platform.repository.OrderTimelineEventRepository;
import com.noura.platform.repository.ProductInventoryRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.security.SecurityUtils;
import com.noura.platform.service.LocationIntelligenceService;
import com.noura.platform.service.CheckoutService;
import com.noura.platform.service.PricingService;
import com.noura.platform.service.AnalyticsEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 128;

    private final UserAccountRepository userAccountRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductInventoryRepository inventoryRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderTimelineEventRepository orderTimelineEventRepository;
    private final B2BCompanyProfileRepository companyProfileRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final StoreRepository storeRepository;
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private final AppProperties appProperties;
    private final PricingService pricingService;
    private final AnalyticsEventService analyticsEventService;
    private final LocationIntelligenceService locationIntelligenceService;
    private final ObjectMapper objectMapper;

    /**
     * Validates checkout.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    public OrderDto checkout(CheckoutRequest request) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(SecurityUtils.currentEmail())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        String idempotencyKey = normalizeIdempotencyKey(request.idempotencyKey());
        if (idempotencyKey != null) {
            Order existingOrder = orderRepository.findByUserAndIdempotencyKey(user, idempotencyKey).orElse(null);
            if (existingOrder != null) {
                return toOrderDto(existingOrder);
            }
        }

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new NotFoundException("CART_NOT_FOUND", "Cart not found"));
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new UnauthorizedException("CART_EMPTY", "Cart is empty");
        }

        CheckoutLocationContext locationContext = resolveCheckoutLocation(user, cart, request);
        Store store = locationContext.store();
        CartTotalsDto totals = pricingService.calculateTotals(cartItems, store, request.couponCode());

        Order order = new Order();
        order.setUser(user);
        order.setStore(store);
        order.setAddressId(locationContext.address() == null ? null : locationContext.address().getId());
        order.setSubtotal(totals.subtotal());
        order.setDiscountAmount(totals.discountAmount());
        order.setShippingAmount(totals.shippingAmount());
        order.setTotalAmount(totals.totalAmount());
        order.setFulfillmentMethod(request.fulfillmentMethod());
        order.setShippingAddressSnapshot(locationContext.shippingAddressSnapshot());
        order.setPaymentReference(request.paymentReference());
        order.setCouponCode(totals.couponCode());
        order.setLocationSnapshotJson(locationContext.locationSnapshotJson());
        order.setMatchedServiceAreaId(locationContext.eligibility() == null ? null : locationContext.eligibility().matchedServiceAreaId());
        order.setEligibilityReason(locationContext.eligibility() == null ? null : locationContext.eligibility().eligibilityReason());
        order.setDeliveryLatitude(locationContext.address() == null ? null : locationContext.address().getLatitude());
        order.setDeliveryLongitude(locationContext.address() == null ? null : locationContext.address().getLongitude());
        order.setAddressValidationStatus(locationContext.validationStatus());
        order.setIdempotencyKey(idempotencyKey);
        order.setStatus(request.b2bInvoice() ? OrderStatus.REVIEWED : OrderStatus.PAID);
        try {
            order = orderRepository.save(order);
        } catch (DataIntegrityViolationException ex) {
            Order existingOrder = resolveExistingOrderForIdempotency(user, idempotencyKey, ex);
            if (existingOrder != null) {
                return toOrderDto(existingOrder);
            }
            throw ex;
        }

        appendTimeline(order, request.b2bInvoice()
                ? "Order submitted for B2B review."
                : "Order confirmed and paid.");

        for (CartItem cartItem : cartItems) {
            if (store != null) {
                reserveInventory(cartItem, store);
            }
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setProductName(cartItem.getProduct().getName());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setLineTotal(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            orderItemRepository.save(orderItem);
        }

        if (request.b2bInvoice()) {
            createApprovalIfRequired(user, order);
        }

        cartItemRepository.deleteByCartId(cart.getId());
        clearCheckoutDraft(cart);
        publishOrderEvent(order);
        trackCheckoutCompleted(order, totals);
        return toOrderDto(order);
    }

    @Override
    @Transactional
    public OrderDto checkoutFromDraft(CheckoutConfirmRequest request) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(SecurityUtils.currentEmail())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new NotFoundException("CART_NOT_FOUND", "Cart not found"));
        CheckoutRequest resolved = resolveCheckoutRequest(request, cart);
        return checkout(resolved);
    }

    /**
     * Creates approval if required.
     *
     * @param user The user context for this operation.
     * @param order The order value.
     */
    private void createApprovalIfRequired(UserAccount user, Order order) {
        companyProfileRepository.findByUser(user).ifPresent(profile -> {
            if (!profile.isApprovalRequired()) {
                return;
            }
            if (order.getTotalAmount().compareTo(profile.getApprovalThreshold()) < 0) {
                return;
            }
            ApprovalRequest approval = new ApprovalRequest();
            approval.setRequester(user);
            approval.setOrder(order);
            approval.setAmount(order.getTotalAmount());
            approval.setStatus(ApprovalStatus.PENDING);
            approvalRequestRepository.save(approval);
        });
    }

    /**
     * Executes publish order event.
     *
     * @param order The order value.
     */
    private void publishOrderEvent(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                order.getUser().getId(),
                order.getStore() == null ? null : order.getStore().getId()
        );
        applicationEventPublisher.publishEvent(event);
        if (appProperties.getKafka().isEnabled()) {
            kafkaTemplate.send(appProperties.getKafka().getTopicOrderCreated(), event);
        }
    }

    private void trackCheckoutCompleted(Order order, CartTotalsDto totals) {
        java.util.Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("orderTotal", order.getTotalAmount());
        metadata.put("subtotal", totals.subtotal());
        metadata.put("discountAmount", totals.discountAmount());
        metadata.put("shippingAmount", totals.shippingAmount());
        metadata.put("promotionCodes", totals.appliedPromotionCodes());
        analyticsEventService.track(new AnalyticsEventRequest(
                AnalyticsEventType.CHECKOUT_COMPLETED,
                null,
                SecurityUtils.currentEmail(),
                null,
                order.getId().toString(),
                totals.appliedPromotionCodes().isEmpty() ? order.getCouponCode() : totals.appliedPromotionCodes().get(0),
                order.getStore() == null ? null : order.getStore().getId().toString(),
                null,
                null,
                null,
                "backend-checkout-service",
                Instant.now(),
                metadata
        ));
    }

    /**
     * Reserves inventory with an atomic stock decrement.
     *
     * @param cartItem The cart item value.
     * @param store The store value.
     */
    private void reserveInventory(CartItem cartItem, Store store) {
        int updatedRows = inventoryRepository.decrementStockIfAvailable(
                cartItem.getProduct().getId(),
                store.getId(),
                cartItem.getQuantity()
        );
        if (updatedRows > 0) {
            return;
        }
        ProductInventory inventory = inventoryRepository.findByProductIdAndStoreId(
                        cartItem.getProduct().getId(),
                        store.getId()
                )
                .orElseThrow(() -> new NotFoundException("INVENTORY_NOT_FOUND", "Inventory not found"));
        if (inventory.getStock() < cartItem.getQuantity()) {
            throw new UnauthorizedException("STOCK_INSUFFICIENT", "Insufficient stock for " + cartItem.getProduct().getName());
        }
    }

    /**
     * Executes append timeline.
     *
     * @param order The order value.
     * @param note The note value.
     */
    private void appendTimeline(Order order, String note) {
        OrderTimelineEvent event = new OrderTimelineEvent();
        event.setOrder(order);
        event.setStatus(order.getStatus());
        event.setRefundStatus(order.getRefundStatus());
        event.setActor(SecurityUtils.currentEmail());
        event.setNote(note);
        orderTimelineEventRepository.save(event);
    }

    private CheckoutRequest resolveCheckoutRequest(CheckoutConfirmRequest request, Cart cart) {
        FulfillmentMethod fulfillmentMethod = request.fulfillmentMethod() != null
                ? request.fulfillmentMethod()
                : cart.getFulfillmentMethod();
        String shippingSnapshot = normalizeSnapshot(request.shippingAddressSnapshot());
        if (shippingSnapshot == null) {
            shippingSnapshot = normalizeSnapshot(cart.getShippingAddressSnapshot());
        }
        if (fulfillmentMethod == null) {
            throw new UnauthorizedException("CHECKOUT_FULFILLMENT_REQUIRED", "Fulfillment method is required");
        }
        String paymentReference = normalizeSnapshot(request.paymentReference());
        if (paymentReference == null) {
            paymentReference = normalizeSnapshot(cart.getPaymentReference());
        }
        String couponCode = normalizeSnapshot(request.couponCode());
        if (couponCode == null) {
            couponCode = normalizeSnapshot(cart.getCouponCode());
        }
        boolean b2bInvoice = request.b2bInvoice() != null
                ? request.b2bInvoice()
                : cart.isB2bInvoice();
        String idempotencyKey = normalizeSnapshot(request.idempotencyKey());
        if (idempotencyKey == null) {
            idempotencyKey = normalizeSnapshot(cart.getIdempotencyKey());
        }
        return new CheckoutRequest(
                fulfillmentMethod,
                request.storeId(),
                request.addressId() != null ? request.addressId() : cart.getAddressId(),
                shippingSnapshot,
                paymentReference,
                couponCode,
                b2bInvoice,
                idempotencyKey
        );
    }

    private Store resolveCheckoutStore(Cart cart, UUID requestedStoreId, boolean authoritative) {
        Store store = cart.getStore();
        if (requestedStoreId != null) {
            if (!authoritative && store != null && !store.getId().equals(requestedStoreId)) {
                throw new UnauthorizedException("STORE_CONFLICT", "Checkout store mismatch");
            }
            if (store == null || !store.getId().equals(requestedStoreId)) {
                Store requested = storeRepository.findById(requestedStoreId)
                        .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
                cart.setStore(requested);
                cartRepository.save(cart);
                store = requested;
            }
        }
        return store;
    }

    private void clearCheckoutDraft(Cart cart) {
        cart.setStore(null);
        cart.setAddressId(null);
        cart.setFulfillmentMethod(null);
        cart.setShippingAddressSnapshot(null);
        cart.setPaymentReference(null);
        cart.setCouponCode(null);
        cart.setB2bInvoice(false);
        cart.setIdempotencyKey(null);
        cartRepository.save(cart);
    }

    private String normalizeSnapshot(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private CheckoutLocationContext resolveCheckoutLocation(UserAccount user, Cart cart, CheckoutRequest request) {
        if (request.fulfillmentMethod() == FulfillmentMethod.DELIVERY) {
            UUID addressId = request.addressId() != null ? request.addressId() : cart.getAddressId();
            if (addressId == null) {
                throw new BadRequestException("CHECKOUT_ADDRESS_REQUIRED", "A saved delivery address is required for delivery checkout.");
            }

            Address address = addressRepository.findByIdAndUser(addressId, user)
                    .orElseThrow(() -> new NotFoundException("ADDRESS_NOT_FOUND", "Address not found"));
            if (address.getLatitude() == null || address.getLongitude() == null) {
                throw new BadRequestException("CHECKOUT_ADDRESS_COORDINATES_REQUIRED", "Delivery address must include coordinates.");
            }

            ServiceEligibilityDto eligibility = locationIntelligenceService.validate(new ServiceAreaValidationRequest(
                    address.getLatitude(),
                    address.getLongitude(),
                    StoreServiceType.DELIVERY,
                    Instant.now(),
                    null
            ));
            AddressValidationStatus validationStatus = toAddressValidationStatus(eligibility);
            address.setValidationStatus(validationStatus);
            addressRepository.save(address);

            if (!eligibility.serviceAvailable()) {
                throw new BadRequestException(
                        "DELIVERY_UNAVAILABLE",
                        "Delivery is unavailable for the selected address: " + eligibility.eligibilityReason()
                );
            }

            UUID storeId = eligibility.matchedStoreId() != null ? eligibility.matchedStoreId() : request.storeId();
            Store store = resolveCheckoutStore(cart, storeId, true);
            if (store == null) {
                throw new BadRequestException("CHECKOUT_STORE_REQUIRED", "No eligible fulfillment store was assigned.");
            }

            return new CheckoutLocationContext(
                    address,
                    store,
                    eligibility,
                    validationStatus,
                    buildLocationSnapshot(address, eligibility),
                    buildShippingAddressSnapshot(address)
            );
        }

        Store pickupStore = resolveCheckoutStore(cart, request.storeId(), false);
        if (pickupStore == null) {
            throw new BadRequestException("CHECKOUT_STORE_REQUIRED", "Pickup checkout requires a selected store.");
        }
        return new CheckoutLocationContext(
                null,
                pickupStore,
                null,
                null,
                null,
                normalizeSnapshot(request.shippingAddressSnapshot()) != null
                        ? normalizeSnapshot(request.shippingAddressSnapshot())
                        : buildPickupAddressSnapshot(pickupStore)
        );
    }

    private String buildShippingAddressSnapshot(Address address) {
        if (address.getFormattedAddress() != null && !address.getFormattedAddress().isBlank()) {
            return address.getFormattedAddress().trim();
        }
        return String.join(
                ", ",
                List.of(
                        address.getFullName(),
                        address.getLine1(),
                        address.getLine2(),
                        address.getDistrict(),
                        address.getCity(),
                        address.getState(),
                        address.getZipCode(),
                        address.getCountry()
                ).stream().filter(value -> value != null && !value.isBlank()).toList()
        );
    }

    private String buildPickupAddressSnapshot(Store store) {
        return String.join(
                ", ",
                List.of(
                        store.getName(),
                        store.getAddressLine1(),
                        store.getCity(),
                        store.getState(),
                        store.getZipCode(),
                        store.getCountry()
                ).stream().filter(value -> value != null && !value.isBlank()).toList()
        );
    }

    private String buildLocationSnapshot(Address address, ServiceEligibilityDto eligibility) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("capturedAt", Instant.now());
        snapshot.put("addressId", address.getId());
        snapshot.put("formattedAddress", address.getFormattedAddress());
        snapshot.put("line1", address.getLine1());
        snapshot.put("line2", address.getLine2());
        snapshot.put("district", address.getDistrict());
        snapshot.put("city", address.getCity());
        snapshot.put("state", address.getState());
        snapshot.put("postalCode", address.getZipCode());
        snapshot.put("country", address.getCountry());
        snapshot.put("latitude", address.getLatitude());
        snapshot.put("longitude", address.getLongitude());
        snapshot.put("placeId", address.getPlaceId());
        snapshot.put("validationStatus", toAddressValidationStatus(eligibility));
        if (eligibility != null) {
            Map<String, Object> eligibilitySnapshot = new LinkedHashMap<>();
            eligibilitySnapshot.put("serviceAvailable", eligibility.serviceAvailable());
            eligibilitySnapshot.put("serviceType", eligibility.serviceType());
            eligibilitySnapshot.put("matchedServiceAreaId", eligibility.matchedServiceAreaId());
            eligibilitySnapshot.put("matchedStoreId", eligibility.matchedStoreId());
            eligibilitySnapshot.put("distanceMeters", eligibility.distanceMeters());
            eligibilitySnapshot.put("eligibilityReason", eligibility.eligibilityReason());
            snapshot.put("eligibility", eligibilitySnapshot);
        }
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize order location snapshot.", ex);
        }
    }

    private AddressValidationStatus toAddressValidationStatus(ServiceEligibilityDto eligibility) {
        if (eligibility == null) {
            return AddressValidationStatus.UNVERIFIED;
        }
        if (eligibility.serviceAvailable()) {
            return AddressValidationStatus.VALID;
        }
        return switch (eligibility.eligibilityReason()) {
            case "SERVICE_AREA_MISS" -> AddressValidationStatus.OUT_OF_SERVICE_AREA;
            case "OUT_OF_RANGE", "OUT_OF_STORE_RADIUS" -> AddressValidationStatus.OUT_OF_STORE_RADIUS;
            case "STORE_CLOSED" -> AddressValidationStatus.STORE_CLOSED;
            default -> AddressValidationStatus.STORE_UNAVAILABLE;
        };
    }

    /**
     * Executes to order dto.
     *
     * @param order The order value.
     * @return The mapped DTO representation.
     */
    private OrderDto toOrderDto(Order order) {
        List<OrderItemDto> items = orderItemRepository.findByOrderId(order.getId()).stream()
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

    /**
     * Executes normalize idempotency key.
     *
     * @param idempotencyKey The idempotency key value.
     * @return The result of normalize idempotency key.
     */
    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        String normalized = idempotencyKey.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw new UnauthorizedException("IDEMPOTENCY_KEY_INVALID", "Idempotency key must be <= 128 characters");
        }
        return normalized;
    }

    /**
     * Resolves existing order for idempotency.
     *
     * @param user The user context for this operation.
     * @param idempotencyKey The idempotency key value.
     * @param ex The ex value.
     * @return The result of resolve existing order for idempotency.
     */
    private Order resolveExistingOrderForIdempotency(
            UserAccount user,
            String idempotencyKey,
            DataIntegrityViolationException ex
    ) {
        if (idempotencyKey == null) {
            return null;
        }
        Order existing = orderRepository.findByUserAndIdempotencyKey(user, idempotencyKey).orElse(null);
        if (existing != null) {
            return existing;
        }
        throw ex;
    }

    private record CheckoutLocationContext(
            Address address,
            Store store,
            ServiceEligibilityDto eligibility,
            AddressValidationStatus validationStatus,
            String locationSnapshotJson,
            String shippingAddressSnapshot
    ) {
    }
}
