package com.noura.payment.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Generic API envelope used by extracted NOURA services.
 *
 * @param success success marker
 * @param message response message
 * @param data payload data
 * @param error error payload
 * @param <T> payload type
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RemoteApiEnvelope<T>(
        Boolean success,
        String message,
        T data,
        RemoteApiError error
) {
}
