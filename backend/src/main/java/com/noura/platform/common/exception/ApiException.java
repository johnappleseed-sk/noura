package com.noura.platform.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    /**
     * Creates a new ApiException instance.
     *
     * @param status The status value.
     * @param code The code value.
     * @param message The message value.
     */
    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
