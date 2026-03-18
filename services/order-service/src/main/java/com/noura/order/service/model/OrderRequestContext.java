package com.noura.order.service.model;

import java.util.Locale;
import java.util.Set;

/**
 * Request-level actor context for order authorization and auditing.
 *
 * @param subject resolved customer subject
 * @param roles resolved role set from gateway-forwarded headers
 * @param internalCall indicates request is trusted internal service call
 */
public record OrderRequestContext(
        String subject,
        Set<String> roles,
        boolean internalCall
) {

    /**
     * Returns whether a customer subject is available.
     *
     * @return {@code true} when subject exists
     */
    public boolean hasSubject() {
        return subject != null && !subject.isBlank();
    }

    /**
     * Returns whether actor can perform admin/internal operations.
     *
     * @return {@code true} when actor is internal or has an admin-like role
     */
    public boolean canManageAllOrders() {
        return internalCall
                || hasRole("ADMIN")
                || hasRole("ROLE_ADMIN")
                || hasRole("SUPER_ADMIN")
                || hasRole("ORDER_MANAGER")
                || hasRole("ROLE_ORDER_MANAGER");
    }

    /**
     * Checks whether the actor has one role code.
     *
     * @param roleCode expected role code
     * @return {@code true} when role code exists
     */
    public boolean hasRole(String roleCode) {
        if (roles == null || roles.isEmpty() || roleCode == null) {
            return false;
        }
        String normalized = roleCode.trim().toUpperCase(Locale.ROOT);
        return roles.stream().anyMatch(role -> normalized.equals(role.toUpperCase(Locale.ROOT)));
    }

    /**
     * Resolves actor identifier for audit fields.
     *
     * @return actor identifier
     */
    public String actorId() {
        if (hasSubject()) {
            return subject.trim();
        }
        return internalCall ? "internal" : "anonymous";
    }
}

