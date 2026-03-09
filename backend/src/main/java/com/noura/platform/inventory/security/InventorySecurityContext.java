package com.noura.platform.inventory.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class InventorySecurityContext {

    private InventorySecurityContext() {
    }

    public static Optional<InventoryUserPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof InventoryUserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }
}
