package com.noura.shipping.exception;

/**
 * Exception thrown when a required shipping resource is not found.
 */
public class NotFoundException extends RuntimeException {

    private final String code;

    /**
     * Creates not-found exception with stable code.
     *
     * @param code stable machine-readable code
     * @param message human-readable detail
     */
    public NotFoundException(String code, String message) {
        super(message);
        this.code = code;
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
