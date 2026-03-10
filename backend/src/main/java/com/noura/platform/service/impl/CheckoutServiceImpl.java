package com.noura.platform.service.impl;

import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.common.exception.UnauthorizedException;
import com.noura.platform.config.AppProperties;
import com.noura.platform.domain.enums.AnalyticsEventType;
import com.noura.platform.domain.entity.ApprovalRequest;
import com.noura.platform.domain.entity.Cart;
import com.noura.platform.domain.entity.CartItem;
import com.noura.platform.domain.entity.Order;
import com.noura.platform.domain.entity.OrderItem;
import com.noura.platform.domain.entity.OrderTimelineEvent;
import com.noura.platform.domain.entity.ProductInventory;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.enums.ApprovalStatus;
import com.noura.platform.domain.enums.OrderStatus;
import com.noura.platform.dto.cart.CartTotalsDto;
import com.noura.platform.dto.analytics.AnalyticsEventRequest;
import com.noura.platform.domain.enums.FulfillmentMethod;
import com.noura.platform.dto.order.CheckoutConfirmRequest;
import com.noura.platform.dto.order.CheckoutRequest;
import com.noura.platform.dto.order.OrderDto;
import com.noura.platform.dto.order.OrderItemDto;
import com.noura.platform.event.OrderCreatedEvent;
import com.noura.platform.mapper.OrderMapper;
import com.noura.platform.repository.ApprovalRequestRepository;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 128;

    private final UserAccountRepository userAccountRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductInventoryRepository inventoryRepository;
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

        Store store = resolveCheckoutStore(cart, request.storeId());
        CartTotalsDto totals = pricingService.calculateTotals(cartItems, store, request.couponCode());

        Order order = new Order();
        order.setUser(user);
        order.setStore(store);
        order.setSubtotal(totals.subtotal());
        order.setDiscountAmount(totals.discountAmount());
        order.setShippingAmount(totals.shippingAmount());
        order.setTotalAmount(totals.totalAmount());
        order.setFulfillmentMethod(request.fulfillmentMethod());
        order.setShippingAddressSnapshot(request.shippingAddressSnapshot());
        order.setPaymentReference(request.paymentReference());
        order.setCouponCode(totals.couponCode());
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
        if (shippingSnapshot == null) {
            throw new UnauthorizedException("CHECKOUT_SHIPPING_REQUIRED", "Shipping address snapshot is required");
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
                shippingSnapshot,
                paymentReference,
                couponCode,
                b2bInvoice,
                idempotencyKey
        );
    }

    private Store resolveCheckoutStore(Cart cart, java.util.UUID requestedStoreId) {
        Store store = cart.getStore();
        if (requestedStoreId != null) {
            if (store != null && !store.getId().equals(requestedStoreId)) {
                throw new UnauthorizedException("STORE_CONFLICT", "Checkout store mismatch");
            }
            if (store == null) {
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
}
