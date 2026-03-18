package com.noura.checkout.exception;

/**
 * Exception thrown when a required checkout resource is not found.
 */
public class NotFoundException extends RuntimeException {

    private final String code;

    /**
     * Creates a not-found exception with a stable machine-readable code.
     *
     * @param code stable machine-readable error code
     * @param message human-readable detail message
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

