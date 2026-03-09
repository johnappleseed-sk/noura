package com.noura.platform.service;

import com.noura.platform.dto.cart.AddCartItemRequest;
import com.noura.platform.dto.cart.ApplyCouponRequest;
import com.noura.platform.dto.cart.CartDto;
import com.noura.platform.dto.cart.UpdateCartItemRequest;
import com.noura.platform.dto.order.CheckoutPaymentRequest;
import com.noura.platform.dto.order.CheckoutShippingRequest;

import java.util.UUID;

public interface CartService {
    /**
     * Retrieves my cart.
     *
     * @return The mapped DTO representation.
     */
    CartDto getMyCart();

    /**
     * Adds item.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    CartDto addItem(AddCartItemRequest request);

    /**
     * Updates item.
     *
     * @param cartItemId The cart item id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    CartDto updateItem(UUID cartItemId, UpdateCartItemRequest request);

    /**
     * Removes item.
     *
     * @param cartItemId The cart item id used to locate the target record.
     * @return The mapped DTO representation.
     */
    CartDto removeItem(UUID cartItemId);

    /**
     * Clears cart.
     *
     * @return The mapped DTO representation.
     */
    CartDto clearCart();

    /**
     * Applies coupon.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    CartDto applyCoupon(ApplyCouponRequest request);

    /**
     * Applies shipping step details to the checkout draft.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    CartDto updateShippingDraft(CheckoutShippingRequest request);

    /**
     * Applies payment step details to the checkout draft.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    CartDto updatePaymentDraft(CheckoutPaymentRequest request);
}
