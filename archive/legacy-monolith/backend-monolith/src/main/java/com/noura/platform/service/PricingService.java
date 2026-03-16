package com.noura.platform.service;

import com.noura.platform.domain.entity.CartItem;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.dto.cart.CartTotalsDto;

import java.util.List;

public interface PricingService {
    /**
     * Calculates totals.
     *
     * @param cartItems The cart items value.
     * @param store The store value.
     * @param couponCode The coupon code value.
     * @return The mapped DTO representation.
     */
    CartTotalsDto calculateTotals(List<CartItem> cartItems, Store store, String couponCode);
}
