package com.noura.platform.service;

import com.noura.platform.dto.order.CheckoutConfirmRequest;
import com.noura.platform.dto.order.CheckoutRequest;
import com.noura.platform.dto.order.OrderDto;

public interface CheckoutService {
    /**
     * Validates checkout.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    OrderDto checkout(CheckoutRequest request);

    /**
     * Confirms checkout using stored draft data when request fields are omitted.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    OrderDto checkoutFromDraft(CheckoutConfirmRequest request);
}
