package com.noura.promotion.service.model;

import java.util.Locale;
import java.util.Set;

/**
 * Request-level actor context for promotion authorization and auditing.
 *
 * @param subject resolved customer or actor subject
 * @param authorizationHeader raw authorization header, when present
 * @param roles resolved gateway-forwarded role set
 * @param internalCall whether request is trusted as an internal service call
 */
public record PromotionRequestContext(
        String subject,
        String authorizationHeader,
        Set<String> roles,
        boolean internalCall
) {

    /**
     * Returns whether a subject is available.
     *
     * @return {@code true} when subject exists
     */
    public boolean hasSubject() {
        return subject != null && !subject.isBlank();
    }

    /**
     * Returns whether actor can perform privileged promotion operations.
     *
     * @return {@code true} when actor is trusted internal or has an admin-like role
     */
    public boolean canManageAllPromotions() {
        return internalCall
                || hasRole("ADMIN")
                || hasRole("ROLE_ADMIN")
                || hasRole("SUPER_ADMIN")
                || hasRole("MARKETING")
                || hasRole("ROLE_MARKETING")
                || hasRole("MARKETING_MANAGER")
                || hasRole("ROLE_MARKETING_MANAGER")
                || hasRole("SALES")
                || hasRole("ROLE_SALES")
                || hasRole("PRICING")
                || hasRole("ROLE_PRICING");
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
