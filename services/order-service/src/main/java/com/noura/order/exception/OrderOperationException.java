package com.noura.order.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception representing business-rule or authorization failures in order workflows.
 */
public class OrderOperationException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    /**
     * Creates an order operation exception.
     *
     * @param status mapped HTTP status
     * @param code machine-readable error code
     * @param message human-readable detail
     */
    public OrderOperationException(HttpStatus status, String code, String message) {
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

