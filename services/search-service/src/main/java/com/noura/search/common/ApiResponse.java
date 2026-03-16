package com.noura.search.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Map;

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

    private static String currentCorrelationId() {
        return MDC.get("correlationId");
    }

    public record ErrorBody(String code, String detail, Map<String, String> validationErrors) {
        public ErrorBody(String code, String detail) {
            this(code, detail, null);
        }
    }
}
