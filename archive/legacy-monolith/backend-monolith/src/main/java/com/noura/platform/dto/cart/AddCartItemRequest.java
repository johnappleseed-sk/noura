package com.noura.platform.dto.cart;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record AddCartItemRequest(
        @NotNull UUID storeProductReferenceId,
        @Positive int quantity
) {
}
