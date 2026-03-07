package com.noura.platform.service;

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
}
