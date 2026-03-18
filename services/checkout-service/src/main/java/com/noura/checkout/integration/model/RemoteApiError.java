package com.noura.checkout.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Generic remote API error body.
 *
 * @param code stable remote error code
 * @param detail remote error detail
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RemoteApiError(
        String code,
        String detail
) {
}

