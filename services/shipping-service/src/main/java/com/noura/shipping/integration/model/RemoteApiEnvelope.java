package com.noura.shipping.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Generic remote API response envelope used for service-to-service lookups.
 *
 * @param success success flag
 * @param message summary message
 * @param data payload body
 * @param error error details
 * @param correlationId correlation identifier
 * @param timestamp response timestamp
 * @param path remote request path
 * @param <T> payload type
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RemoteApiEnvelope<T>(
        Boolean success,
        String message,
        T data,
        RemoteApiError error,
        String correlationId,
        Instant timestamp,
        String path
) {
}
