package com.noura.review.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when review business rules or request authorization fail.
 */
public class ReviewOperationException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    /**
     * Creates review operation exception.
     *
     * @param status mapped HTTP status
     * @param code stable machine-readable code
     * @param message human-readable detail
     */
    public ReviewOperationException(HttpStatus status, String code, String message) {
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
