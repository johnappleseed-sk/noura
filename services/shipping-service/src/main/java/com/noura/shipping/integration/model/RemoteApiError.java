package com.noura.shipping.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Generic remote API error payload model.
 *
 * @param code machine-readable code
 * @param detail human-readable detail
 * @param validationErrors optional validation errors
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RemoteApiError(
        String code,
        String detail,
        Map<String, String> validationErrors
) {
}
