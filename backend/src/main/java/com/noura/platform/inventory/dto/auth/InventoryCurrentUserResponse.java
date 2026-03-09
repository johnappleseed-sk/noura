package com.noura.platform.inventory.dto.auth;

import java.util.List;

public record InventoryCurrentUserResponse(
        String id,
        String username,
        String email,
        String fullName,
        String status,
        List<String> roles,
        List<String> permissions
) {
}
