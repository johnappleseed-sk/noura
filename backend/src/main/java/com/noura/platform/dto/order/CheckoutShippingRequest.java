package com.noura.platform.dto.order;

import com.noura.platform.domain.enums.FulfillmentMethod;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Captures shipping step inputs for a multi-step checkout draft.
 */
public record CheckoutShippingRequest(
        @NotNull FulfillmentMethod fulfillmentMethod,
        UUID storeId,
        UUID addressId,
        String shippingAddressSnapshot
) {
}
