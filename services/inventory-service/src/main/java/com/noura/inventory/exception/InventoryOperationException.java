package com.noura.inventory.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a stock operation violates business constraints.
 *
 * <p>Examples include insufficient available stock, insufficient reserved stock,
 * and attempts to create negative balances.</p>
 */
public class InventoryOperationException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    /**
     * Creates an inventory operation exception with HTTP mapping metadata.
     *
     * @param status HTTP status to be returned by the API layer
     * @param code stable machine-readable error code
     * @param message human-readable message
     */
    public InventoryOperationException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /**
     * Returns the HTTP status associated with the operation failure.
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
