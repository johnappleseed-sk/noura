package com.noura.order.exception;

/**
 * Exception thrown when a requested resource is not found.
 */
public class NotFoundException extends RuntimeException {

    private final String code;

    /**
     * Creates a not-found exception.
     *
     * @param code machine-readable error code
     * @param message human-readable error message
     */
    public NotFoundException(String code, String message) {
        super(message);
        this.code = code;
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

