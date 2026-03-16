package com.noura.cart.dto.cart;

/**
 * Command payload for revalidating all cart lines.
 *
 * @param strict when true, the refresh fails on dependency errors; when false, lines are marked UNKNOWN
 */
public record RefreshCartRequest(Boolean strict) {
}
