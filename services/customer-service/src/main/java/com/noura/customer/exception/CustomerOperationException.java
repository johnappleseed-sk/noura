package com.noura.customer.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception representing business-rule or integration failures in customer flows.
 */
public class CustomerOperationException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    /**
     * Creates customer operation exception.
     *
     * @param status HTTP status
     * @param code machine-readable error code
     * @param message human-readable message
     */
    public CustomerOperationException(HttpStatus status, String code, String message) {
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
     * Returns machine-readable error code.
     *
     * @return error code
     */
    public String getCode() {
        return code;
    }
}
