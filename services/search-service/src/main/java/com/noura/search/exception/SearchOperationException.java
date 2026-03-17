package com.noura.search.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when search-service business rules or internal endpoint authorization fail.
 */
public class SearchOperationException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    /**
     * Creates a search operation exception.
     *
     * @param status mapped HTTP status
     * @param code stable machine-readable code
     * @param message human-readable detail
     */
    public SearchOperationException(HttpStatus status, String code, String message) {
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
     * Returns the stable machine-readable error code.
     *
     * @return error code
     */
    public String getCode() {
        return code;
    }
}
