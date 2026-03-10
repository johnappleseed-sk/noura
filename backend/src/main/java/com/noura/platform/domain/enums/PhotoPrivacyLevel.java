package com.noura.platform.domain.enums;

/**
 * Controls how photo-derived location should be treated for privacy and admin visibility.
 */
public enum PhotoPrivacyLevel {
    /**
     * Exact coordinates can be stored and shown to privileged admins only.
     */
    INTERNAL,

    /**
     * Coordinates may be rounded/coarsened, suitable for analytics without precise pinpointing.
     */
    COARSE,

    /**
     * Location should not be stored.
     */
    NONE
}

