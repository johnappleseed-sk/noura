package com.noura.pricing.exception;

/**
 * Exception thrown when a requested pricing resource cannot be found.
 */
public class NotFoundException extends RuntimeException {

    private final String code;

    /**
     * Creates a typed not-found exception.
     *
     * @param code stable machine-readable error code
     * @param message human-readable error message
     */
    public NotFoundException(String code, String message) {
        super(message);
        this.code = code;
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

