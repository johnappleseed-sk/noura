package com.noura.platform.common.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {
    /**
     * Creates a new BadRequestException instance.
     *
     * @param code The code value.
     * @param message The message value.
     */
    public BadRequestException(String code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
    }
}
