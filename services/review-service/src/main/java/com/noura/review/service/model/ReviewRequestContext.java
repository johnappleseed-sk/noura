package com.noura.review.service.model;

import java.util.Locale;
import java.util.Set;

/**
 * Request-level actor context for review submission and moderation authorization.
 *
 * @param subject resolved customer or actor subject
 * @param username resolved gateway-forwarded username
 * @param authorizationHeader raw authorization header, when present
 * @param roles resolved gateway-forwarded role set
 * @param internalCall whether request is trusted as an internal service call
 * @param remoteAddress resolved client IP or forwarding-chain origin
 * @param userAgent request user agent
 */
public record ReviewRequestContext(
        String subject,
        String username,
        String authorizationHeader,
        Set<String> roles,
        boolean internalCall,
        String remoteAddress,
        String userAgent
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
     * Returns whether the actor can submit reviews.
     *
     * @return {@code true} when submission is allowed
     */
    public boolean canSubmitReviews() {
        return internalCall || hasSubject();
    }

    /**
     * Returns whether actor can perform privileged moderation operations.
     *
     * @return {@code true} when actor is trusted internal or has a moderation-capable role
     */
    public boolean canModerateReviews() {
        return internalCall
                || hasRole("ADMIN")
                || hasRole("ROLE_ADMIN")
                || hasRole("SUPER_ADMIN")
                || hasRole("ROLE_SUPER_ADMIN")
                || hasRole("MODERATOR")
                || hasRole("ROLE_MODERATOR")
                || hasRole("CONTENT_MODERATOR")
                || hasRole("ROLE_CONTENT_MODERATOR")
                || hasRole("SUPPORT")
                || hasRole("ROLE_SUPPORT")
                || hasRole("TRUST_AND_SAFETY")
                || hasRole("ROLE_TRUST_AND_SAFETY");
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

    /**
     * Resolves the display name stored on review submissions.
     *
     * @return display name
     */
    public String reviewAuthorName() {
        if (username != null && !username.isBlank()) {
            return username.trim();
        }
        return "Customer";
    }
}
