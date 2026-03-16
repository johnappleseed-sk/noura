package com.company.platform.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        ApiError error,
        Instant timestamp,
        String correlationId,
        String path
) {

    public static <T> ApiResponse<T> ok(String message, T data) {
        return ok(message, data, null);
    }

    public static <T> ApiResponse<T> ok(String message, T data, String path) {
        return new ApiResponse<>(
                true,
                message,
                data,
                null,
                Instant.now(),
                MDC.get("correlationId"),
                path
        );
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return ok(message, data);
    }

    public static <T> ApiResponse<T> fail(String message, String code, String detail) {
        return fail(message, code, detail, (String) null);
    }

    public static <T> ApiResponse<T> fail(String message, String code, String detail, String path) {
        return new ApiResponse<>(
                false,
                message,
                null,
                new ApiError(code, detail, null),
                Instant.now(),
                MDC.get("correlationId"),
                path
        );
    }

    public static <T> ApiResponse<T> fail(String message, String code, String detail, java.util.Map<String, String> validationErrors) {
        return fail(message, code, detail, validationErrors, null);
    }

    public static <T> ApiResponse<T> fail(
            String message,
            String code,
            String detail,
            java.util.Map<String, String> validationErrors,
            String path
    ) {
        return new ApiResponse<>(
                false,
                message,
                null,
                new ApiError(code, detail, validationErrors),
                Instant.now(),
                MDC.get("correlationId"),
                path
        );
    }
}
