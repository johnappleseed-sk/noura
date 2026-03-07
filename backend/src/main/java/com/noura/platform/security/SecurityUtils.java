package com.noura.platform.security;

import com.noura.platform.common.exception.UnauthorizedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
}
