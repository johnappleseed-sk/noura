package com.noura.platform.inventory.dto.auth;

import java.time.Instant;

public record InventoryAuthResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        InventoryCurrentUserResponse user
) {
}
