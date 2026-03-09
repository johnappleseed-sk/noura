package com.noura.platform.service.impl;

import com.noura.platform.commerce.cart.application.StorefrontCartService;
import com.noura.platform.commerce.orders.application.StorefrontOrderService;
import com.noura.platform.dto.cart.AddCartItemRequest;
import com.noura.platform.dto.cart.ApplyCouponRequest;
import com.noura.platform.dto.cart.CartDto;
import com.noura.platform.dto.cart.UpdateCartItemRequest;
import com.noura.platform.dto.order.CheckoutConfirmRequest;
import com.noura.platform.dto.order.CheckoutPaymentRequest;
import com.noura.platform.dto.order.CheckoutRequest;
import com.noura.platform.dto.order.CheckoutShippingRequest;
import com.noura.platform.dto.order.CheckoutStepPreviewDto;
import com.noura.platform.dto.order.OrderDto;
import com.noura.platform.dto.order.OrderTimelineEventDto;
import com.noura.platform.dto.order.UpdateOrderStatusRequest;
import com.noura.platform.dto.storefront.StorefrontAddCartItemRequest;
import com.noura.platform.dto.storefront.StorefrontCartDto;
import com.noura.platform.dto.storefront.StorefrontCheckoutRequest;
import com.noura.platform.dto.storefront.StorefrontOrderResult;
import com.noura.platform.dto.storefront.StorefrontOrderSummaryDto;
import com.noura.platform.dto.storefront.StorefrontUpdateCartItemRequest;
import com.noura.platform.service.CartService;
import com.noura.platform.service.CheckoutService;
import com.noura.platform.service.OrderService;
import com.noura.platform.service.UnifiedOrderService;
import com.noura.platform.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Canonical order facade implementation — Stage 3+4 consolidation.
 *
 * All order-related and cart-related calls across platform and commerce trees
 * are routed through this single service boundary.
 */
@Service
@RequiredArgsConstructor
public class UnifiedOrderServiceImpl implements UnifiedOrderService {

    private final OrderService platformOrderService;
    private final CheckoutService platformCheckoutService;
    private final UserAccountService userAccountService;
    private final CartService cartService;
    private final ObjectProvider<StorefrontOrderService> storefrontOrderServiceProvider;
    private final ObjectProvider<StorefrontCartService> storefrontCartServiceProvider;

    // --- Platform admin order management ---

    @Override
    public Page<OrderDto> adminOrders(Pageable pageable) {
        return platformOrderService.adminOrders(pageable);
    }

    @Override
    public OrderDto getPlatformOrderById(UUID orderId) {
        return platformOrderService.getById(orderId);
    }

    @Override
    public List<OrderTimelineEventDto> orderTimeline(UUID orderId) {
        return platformOrderService.orderTimeline(orderId);
    }

    @Override
    public OrderDto updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request) {
        return platformOrderService.updateStatus(orderId, request);
    }

    // --- Platform checkout workflow ---

    @Override
    public CheckoutStepPreviewDto reviewCheckoutStep() {
        CartDto cart = cartService.getMyCart();
        return new CheckoutStepPreviewDto(
                "review",
                "shipping",
                "Validate items, coupon and totals before shipping.",
                cart
        );
    }

    @Override
    public CheckoutStepPreviewDto shippingCheckoutStep(CheckoutShippingRequest request) {
        CartDto cart = cartService.updateShippingDraft(request);
        return new CheckoutStepPreviewDto(
                "shipping",
                "payment",
                "Select pickup vs delivery and verify address.",
                cart
        );
    }

    @Override
    public CheckoutStepPreviewDto paymentCheckoutStep(CheckoutPaymentRequest request) {
        CartDto cart = cartService.updatePaymentDraft(request);
        return new CheckoutStepPreviewDto(
                "payment",
                "confirm",
                "Attach payment reference or B2B invoice option.",
                cart
        );
    }

    @Override
    public OrderDto confirmCheckout(CheckoutConfirmRequest request) {
        return platformCheckoutService.checkoutFromDraft(request);
    }

    @Override
    public OrderDto checkout(CheckoutRequest request) {
        return platformCheckoutService.checkout(request);
    }

    // --- Platform cart operations (Stage 4) ---

    @Override
    public CartDto getMyCart() {
        return cartService.getMyCart();
    }

    @Override
    public CartDto addCartItem(AddCartItemRequest request) {
        return cartService.addItem(request);
    }

    @Override
    public CartDto updateCartItem(UUID cartItemId, UpdateCartItemRequest request) {
        return cartService.updateItem(cartItemId, request);
    }

    @Override
    public CartDto removeCartItem(UUID cartItemId) {
        return cartService.removeItem(cartItemId);
    }

    @Override
    public CartDto clearCart() {
        return cartService.clearCart();
    }

    @Override
    public CartDto applyCoupon(ApplyCouponRequest request) {
        return cartService.applyCoupon(request);
    }

    // --- Platform account order history ---

    @Override
    public List<OrderDto> myOrderHistory() {
        return userAccountService.myOrderHistory();
    }

    @Override
    public List<OrderDto> quickReorder(UUID orderId) {
        return userAccountService.quickReorder(orderId);
    }

    // --- Storefront (commerce) order operations ---

    @Override
    public StorefrontOrderResult checkoutStorefront(
            Long customerId,
            StorefrontCheckoutRequest request
    ) {
        return storefrontOrderService().checkout(customerId, request);
    }

    @Override
    public List<StorefrontOrderSummaryDto> listStorefrontOrders(Long customerId) {
        return storefrontOrderService().listOrders(customerId);
    }

    @Override
    public StorefrontOrderResult getStorefrontOrder(Long customerId, Long orderId) {
        return storefrontOrderService().getOrder(customerId, orderId);
    }

    @Override
    public StorefrontOrderResult cancelStorefrontOrder(Long customerId, Long orderId) {
        return storefrontOrderService().cancel(customerId, orderId);
    }

    // --- Storefront (commerce) cart operations (Stage 4) ---

    @Override
    public StorefrontCartDto getOrCreateStorefrontCart(Long customerId) {
        return storefrontCartService().getOrCreateCart(customerId);
    }

    @Override
    public StorefrontCartDto addStorefrontCartItem(Long customerId, StorefrontAddCartItemRequest request) {
        return storefrontCartService().addItem(customerId, request);
    }

    @Override
    public StorefrontCartDto updateStorefrontCartItem(Long customerId, Long itemId, StorefrontUpdateCartItemRequest request) {
        return storefrontCartService().updateItem(customerId, itemId, request);
    }

    @Override
    public void removeStorefrontCartItem(Long customerId, Long itemId) {
        storefrontCartService().removeItem(customerId, itemId);
    }

    @Override
    public StorefrontCartDto clearStorefrontCart(Long customerId) {
        return storefrontCartService().clearCart(customerId);
    }

    private StorefrontOrderService storefrontOrderService() {
        StorefrontOrderService service = storefrontOrderServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("Legacy storefront order service is not active in the current runtime profile.");
        }
        return service;
    }

    private StorefrontCartService storefrontCartService() {
        StorefrontCartService service = storefrontCartServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("Legacy storefront cart service is not active in the current runtime profile.");
        }
        return service;
    }
}
