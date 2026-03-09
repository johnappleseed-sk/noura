package com.noura.platform.service;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Canonical order facade — Stage 3+4 consolidation.
 *
 * Delegates to:
 *   - platform OrderService (admin orders, status transitions, timeline)
 *   - platform CheckoutService (checkout workflow)
 *   - platform CartService (platform cart operations + checkout step previews)
 *   - platform UserAccountService (order history, quick-reorder)
 *   - commerce StorefrontOrderService (storefront checkout, list, get, cancel)
 *   - commerce StorefrontCartService (storefront cart operations)
 */
public interface UnifiedOrderService {

    // --- Platform admin order management ---

    Page<OrderDto> adminOrders(Pageable pageable);

    OrderDto getPlatformOrderById(UUID orderId);

    List<OrderTimelineEventDto> orderTimeline(UUID orderId);

    OrderDto updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request);

    // --- Platform checkout workflow ---

    CheckoutStepPreviewDto reviewCheckoutStep();

    CheckoutStepPreviewDto shippingCheckoutStep(CheckoutShippingRequest request);

    CheckoutStepPreviewDto paymentCheckoutStep(CheckoutPaymentRequest request);

    OrderDto confirmCheckout(CheckoutConfirmRequest request);

    OrderDto checkout(CheckoutRequest request);

    // --- Platform cart operations (Stage 4) ---

    CartDto getMyCart();

    CartDto addCartItem(AddCartItemRequest request);

    CartDto updateCartItem(UUID cartItemId, UpdateCartItemRequest request);

    CartDto removeCartItem(UUID cartItemId);

    CartDto clearCart();

    CartDto applyCoupon(ApplyCouponRequest request);

    // --- Platform account order history ---

    List<OrderDto> myOrderHistory();

    List<OrderDto> quickReorder(UUID orderId);

    // --- Storefront (commerce) order operations ---

    StorefrontOrderResult checkoutStorefront(
            Long customerId,
            StorefrontCheckoutRequest request
    );

    List<StorefrontOrderSummaryDto> listStorefrontOrders(Long customerId);

    StorefrontOrderResult getStorefrontOrder(Long customerId, Long orderId);

    StorefrontOrderResult cancelStorefrontOrder(Long customerId, Long orderId);

    // --- Storefront (commerce) cart operations (Stage 4) ---

    StorefrontCartDto getOrCreateStorefrontCart(Long customerId);

    StorefrontCartDto addStorefrontCartItem(Long customerId, StorefrontAddCartItemRequest request);

    StorefrontCartDto updateStorefrontCartItem(Long customerId, Long itemId, StorefrontUpdateCartItemRequest request);

    void removeStorefrontCartItem(Long customerId, Long itemId);

    StorefrontCartDto clearStorefrontCart(Long customerId);
}
