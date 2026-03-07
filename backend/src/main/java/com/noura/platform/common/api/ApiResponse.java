package com.noura.platform.common.api;

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

    /**
     * Executes ok.
     *
     * @param message The message value.
     * @param data The data value.
     * @param path The path value.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    public static <T> ApiResponse<T> ok(String message, T data, String path) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .path(path)
                .build();
    }

    /**
     * Executes fail.
     *
     * @param message The message value.
     * @param code The code value.
     * @param detail The detail value.
     * @param path The path value.
     * @return A standard API response envelope containing operation data and request metadata.
     */
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
