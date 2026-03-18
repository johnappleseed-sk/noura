package com.noura.checkout.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when checkout orchestration or integration rules are violated.
 */
public class CheckoutOperationException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    /**
     * Creates a checkout operation exception with HTTP mapping metadata.
     *
     * @param status HTTP status to be returned by the API layer
     * @param code stable machine-readable error code
     * @param message human-readable detail message
     */
    public CheckoutOperationException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /**
     * Returns the HTTP status associated with the operation failure.
     *
     * @return HTTP status
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * Returns the stable machine-readable error code.
     *
     * @return error code
     */
    public String getCode() {
        return code;
    }
}

