package com.noura.checkout.service.model;

import java.util.Locale;
import java.util.Set;

/**
 * Request-level actor context resolved from inbound request headers.
 *
 * @param subject resolved customer subject
 * @param authorizationHeader raw authorization header, when present
 * @param correlationId resolved correlation identifier
 * @param roles normalized role set forwarded by gateway
 */
public record CheckoutRequestContext(
        String subject,
        String authorizationHeader,
        String correlationId,
        Set<String> roles
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
     * Resolves actor identifier for audit fields.
     *
     * @return actor identifier
     */
    public String actorId() {
        return hasSubject() ? subject.trim() : "anonymous";
    }

    /**
     * Checks whether the actor has one role code.
     *
     * @param roleCode expected role code
     * @return {@code true} when the role code exists in context
     */
    public boolean hasRole(String roleCode) {
        if (roles == null || roles.isEmpty() || roleCode == null || roleCode.isBlank()) {
            return false;
        }
        String normalized = roleCode.trim().toUpperCase(Locale.ROOT);
        return roles.stream().map(value -> value.toUpperCase(Locale.ROOT)).anyMatch(normalized::equals);
    }
}

