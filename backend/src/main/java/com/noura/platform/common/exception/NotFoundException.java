package com.noura.platform.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {
    /**
     * Creates a new NotFoundException instance.
     *
     * @param code The code value.
     * @param message The message value.
     */
    public NotFoundException(String code, String message) {
        super(HttpStatus.NOT_FOUND, code, message);
    }
}
