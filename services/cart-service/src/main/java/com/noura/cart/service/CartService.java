package com.noura.cart.service;

import com.noura.cart.dto.cart.AddCartItemRequest;
import com.noura.cart.dto.cart.CartResponse;
import com.noura.cart.dto.cart.MergeGuestCartRequest;
import com.noura.cart.dto.cart.UpdateCartItemQuantityRequest;
import com.noura.cart.service.model.CartContext;

import java.util.UUID;

/**
 * Cart application service contract for storefront cart workflows.
 */
public interface CartService {

    /**
     * Returns active cart for the given context.
     *
     * @param context ownership context
     * @return active cart response
     */
    CartResponse getCart(CartContext context);

    /**
     * Adds a line item to the current cart.
     *
     * @param context ownership context
     * @param request add-item command
     * @return updated cart response
     */
    CartResponse addItem(CartContext context, AddCartItemRequest request);

    /**
     * Replaces line item quantity.
     *
     * @param context ownership context
     * @param itemId line item identifier
     * @param request quantity update command
     * @return updated cart response
     */
    CartResponse updateItemQuantity(CartContext context, UUID itemId, UpdateCartItemQuantityRequest request);

    /**
     * Removes one line item from cart.
     *
     * @param context ownership context
     * @param itemId line item identifier
     * @return updated cart response
     */
    CartResponse removeItem(CartContext context, UUID itemId);

    /**
     * Clears all line items in cart.
     *
     * @param context ownership context
     * @return updated cart response
     */
    CartResponse clear(CartContext context);

    /**
     * Merges a guest cart into the current customer cart.
     *
     * @param context ownership context
     * @param request merge command
     * @return updated cart response
     */
    CartResponse mergeGuestCart(CartContext context, MergeGuestCartRequest request);

    /**
     * Revalidates all lines against catalog/pricing/inventory and refreshes totals.
     *
     * @param context ownership context
     * @param strict when true, dependency failures throw operation errors
     * @return refreshed cart response
     */
    CartResponse refresh(CartContext context, boolean strict);
}
