package com.noura.payment.service.model;

import java.util.Locale;
import java.util.Set;

/**
 * Request-level actor context for payment authorization and auditing.
 *
 * @param subject resolved customer subject
 * @param authorizationHeader raw authorization header, when present
 * @param roles resolved gateway-forwarded role set
 * @param internalCall whether request is trusted as an internal service call
 */
public record PaymentRequestContext(
        String subject,
        String authorizationHeader,
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
     * Returns whether actor can perform privileged admin/internal payment operations.
     *
     * @return {@code true} when actor is trusted internal or has an admin-like role
     */
    public boolean canManageAllPayments() {
        return internalCall
                || hasRole("ADMIN")
                || hasRole("ROLE_ADMIN")
                || hasRole("SUPER_ADMIN")
                || hasRole("ORDER_MANAGER")
                || hasRole("ROLE_ORDER_MANAGER")
                || hasRole("FINANCE")
                || hasRole("ROLE_FINANCE")
                || hasRole("FINANCE_OFFICER")
                || hasRole("ROLE_FINANCE_OFFICER");
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
