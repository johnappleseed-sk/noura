package com.noura.shipping.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when shipping business rules or carrier operations fail.
 */
public class ShippingOperationException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    /**
     * Creates shipping operation exception.
     *
     * @param status mapped HTTP status
     * @param code stable machine-readable code
     * @param message human-readable detail
     */
    public ShippingOperationException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /**
     * Returns mapped HTTP status.
     *
     * @return HTTP status
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * Returns machine-readable code.
     *
     * @return error code
     */
    public String getCode() {
        return code;
    }
}
