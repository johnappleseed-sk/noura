package com.noura.platform.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApiException {
    /**
     * Creates a new ForbiddenException instance.
     *
     * @param code The code value.
     * @param message The message value.
     */
    public ForbiddenException(String code, String message) {
        super(HttpStatus.FORBIDDEN, code, message);
    }
}
