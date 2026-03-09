package com.noura.platform.inventory.security;

import java.util.List;

public record InventoryUserPrincipal(
        String userId,
        String username,
        String email,
        String fullName,
        List<String> roles,
        List<String> permissions
) {
}
