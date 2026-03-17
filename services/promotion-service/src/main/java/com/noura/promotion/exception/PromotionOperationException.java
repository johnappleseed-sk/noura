package com.noura.promotion.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when promotion business rules or request authorization fail.
 */
public class PromotionOperationException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    /**
     * Creates promotion operation exception.
     *
     * @param status mapped HTTP status
     * @param code stable machine-readable code
     * @param message human-readable detail
     */
    public PromotionOperationException(HttpStatus status, String code, String message) {
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
