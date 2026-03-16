package com.noura.platform.security;

import com.noura.platform.common.exception.UnauthorizedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    /**
     * Creates a new SecurityUtils instance.
     */
    private SecurityUtils() {
    }

    /**
     * Executes current email.
     *
     * @return The result of current email.
     */
    public static String currentEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("AUTH_REQUIRED", "Authentication required");
        }
        return authentication.getName();
    }

    /**
     * Executes current email optional.
     *
     * @return The optional authenticated email if available.
     */
    public static Optional<String> currentEmailOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        String name = authentication.getName();
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(name);
    }

    /**
     * Executes current email or system.
     *
     * @return The authenticated email, or {@code system} when no authenticated principal exists.
     */
    public static String currentEmailOrSystem() {
        return currentEmailOptional().orElse("system");
    }
}
