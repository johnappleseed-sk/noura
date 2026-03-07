package com.noura.platform.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApiException {
    /**
     * Creates a new UnauthorizedException instance.
     *
     * @param code The code value.
     * @param message The message value.
     */
    public UnauthorizedException(String code, String message) {
        super(HttpStatus.UNAUTHORIZED, code, message);
    }
}
