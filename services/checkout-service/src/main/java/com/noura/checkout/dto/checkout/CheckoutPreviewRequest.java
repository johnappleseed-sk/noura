package com.noura.checkout.dto.checkout;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Command payload for checkout preview orchestration.
 *
 * @param storeId optional store/location identifier override
 * @param addressId optional shipping address identifier override
 * @param couponCode optional coupon code placeholder for totals extension
 */
public record CheckoutPreviewRequest(
        UUID storeId,
        UUID addressId,
        @Size(max = 80) String couponCode
) {
}

