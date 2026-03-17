package com.noura.promotion.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Map;

/**
 * Standard API envelope returned by promotion-service endpoints.
 *
 * @param <T> payload type for successful responses
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final ErrorBody error;
    private final String correlationId;
    private final Instant timestamp;
    private final String path;

    /**
     * Builds a successful API response.
     *
     * @param message client-facing summary message
     * @param data payload data
     * @param path request path
     * @param <T> payload type
     * @return success envelope
     */
    public static <T> ApiResponse<T> ok(String message, T data, String path) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .correlationId(currentCorrelationId())
                .timestamp(Instant.now())
                .path(path)
                .build();
    }

    /**
     * Builds an error API response without field-level validation errors.
     *
     * @param message client-facing summary message
     * @param code stable machine-readable code
     * @param detail human-readable detail
     * @param path request path
     * @param <T> payload type
     * @return failure envelope
     */
    public static <T> ApiResponse<T> fail(String message, String code, String detail, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(new ErrorBody(code, detail))
                .correlationId(currentCorrelationId())
                .timestamp(Instant.now())
                .path(path)
                .build();
    }

    /**
     * Builds an error API response with field-level validation errors.
     *
     * @param message client-facing summary message
     * @param code stable machine-readable code
     * @param detail human-readable detail
     * @param validationErrors field-level validation errors
     * @param path request path
     * @param <T> payload type
     * @return failure envelope
     */
    public static <T> ApiResponse<T> fail(
            String message,
            String code,
            String detail,
            Map<String, String> validationErrors,
            String path
    ) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(new ErrorBody(code, detail, validationErrors))
                .correlationId(currentCorrelationId())
                .timestamp(Instant.now())
                .path(path)
                .build();
    }

    /**
     * Reads the current correlation ID from MDC.
     *
     * @return correlation ID or {@code null}
     */
    private static String currentCorrelationId() {
        return MDC.get("correlationId");
    }

    /**
     * Structured error payload for failed requests.
     *
     * @param code machine-readable error code
     * @param detail human-readable detail
     * @param validationErrors optional field-level validation errors
     */
    public record ErrorBody(String code, String detail, Map<String, String> validationErrors) {

        /**
         * Creates an error body without validation details.
         *
         * @param code machine-readable error code
         * @param detail human-readable detail
         */
        public ErrorBody(String code, String detail) {
            this(code, detail, null);
        }
    }
}
