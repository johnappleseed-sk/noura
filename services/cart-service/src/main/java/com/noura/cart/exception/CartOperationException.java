package com.noura.cart.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for cart business-rule violations and downstream integration failures.
 */
public class CartOperationException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    /**
     * Creates a cart operation exception.
     *
     * @param status HTTP status to be returned
     * @param code stable machine-readable error code
     * @param message human-readable message
     */
    public CartOperationException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /**
     * Returns HTTP status associated with this error.
     *
     * @return HTTP status
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * Returns stable machine-readable error code.
     *
     * @return error code
     */
    public String getCode() {
        return code;
    }
}
