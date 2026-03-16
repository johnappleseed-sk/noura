package com.noura.cart.dto.cart;

import jakarta.validation.constraints.NotBlank;

/**
 * Command payload for merging a guest cart into a customer cart.
 *
 * @param guestToken source guest cart token
 */
public record MergeGuestCartRequest(
        @NotBlank String guestToken
) {
}
