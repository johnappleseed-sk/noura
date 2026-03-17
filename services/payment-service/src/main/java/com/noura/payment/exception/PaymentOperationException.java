package com.noura.payment.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when payment business rules or provider operations fail.
 */
public class PaymentOperationException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    /**
     * Creates payment operation exception.
     *
     * @param status mapped HTTP status
     * @param code stable machine-readable code
     * @param message human-readable detail
     */
    public PaymentOperationException(HttpStatus status, String code, String message) {
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
