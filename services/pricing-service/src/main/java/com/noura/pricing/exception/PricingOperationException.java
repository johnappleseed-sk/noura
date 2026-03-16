package com.noura.pricing.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a pricing operation violates business constraints.
 */
public class PricingOperationException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    /**
     * Creates a pricing operation exception.
     *
     * @param status HTTP status to return
     * @param code stable machine-readable error code
     * @param message human-readable message
     */
    public PricingOperationException(HttpStatus status, String code, String message) {
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
     * Returns stable machine-readable error code.
     *
     * @return error code
     */
    public String getCode() {
        return code;
    }
}

