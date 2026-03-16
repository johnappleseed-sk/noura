package com.noura.notification.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final boolean success;
    private final String message;
    private final T data;
    private final ErrorBody error;
    private final Instant timestamp;
    private final String path;

    public static <T> ApiResponse<T> ok(String message, T data, String path) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .path(path)
                .build();
    }

    public static <T> ApiResponse<T> fail(String message, String code, String detail, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(new ErrorBody(code, detail))
                .timestamp(Instant.now())
                .path(path)
                .build();
    }

    public record ErrorBody(String code, String detail) {
    }
}

