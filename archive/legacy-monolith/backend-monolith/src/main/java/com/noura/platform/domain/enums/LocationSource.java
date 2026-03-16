package com.noura.platform.domain.enums;

/**
 * Declares where a coordinate/address signal originated.
 *
 * <p>These values are persisted for auditability and to prevent over-trusting user-supplied data.</p>
 */
public enum LocationSource {
    GPS,
    BROWSER,
    MANUAL_PIN,
    REVERSE_GEOCODE,
    PHOTO_EXIF,
    ADMIN_INPUT,
    INFERRED
}

