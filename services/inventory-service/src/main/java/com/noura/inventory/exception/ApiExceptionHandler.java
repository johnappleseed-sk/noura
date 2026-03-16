package com.noura.inventory.exception;

import com.noura.inventory.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception-to-response mapper for inventory APIs.
 *
 * <p>This advice enforces a consistent error envelope across validation, domain, and
 * unexpected failures.</p>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Maps request body validation failures to HTTP 400.
     *
     * @param ex validation exception produced by Spring MVC
     * @param request current HTTP request
     * @return standard error envelope with field-level validation details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(
                        "Validation failed",
                        "VALIDATION_ERROR",
                        "One or more request fields are invalid",
                        errors,
                        request.getRequestURI()
                ));
    }

    /**
     * Maps query/path validation failures to HTTP 400.
     *
     * @param ex validation exception for method constraints
     * @param request current HTTP request
     * @return standard validation error envelope
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(
                        "Validation failed",
                        "VALIDATION_ERROR",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Maps not-found errors to HTTP 404.
     *
     * @param ex domain not-found exception
     * @param request current HTTP request
     * @return standard 404 error envelope
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(404)
                .body(ApiResponse.fail(
                        "Resource not found",
                        ex.getCode(),
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Maps stock business-rule failures to their associated HTTP status.
     *
     * @param ex inventory business exception
     * @param request current HTTP request
     * @return standard operation failure envelope
     */
    @ExceptionHandler(InventoryOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleOperation(InventoryOperationException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.fail(
                        "Inventory operation rejected",
                        ex.getCode(),
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Fallback handler for unexpected errors.
     *
     * @param ex unexpected exception
     * @param request current HTTP request
     * @return generic internal server error envelope
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(500)
                .body(ApiResponse.fail(
                        "Internal server error",
                        "INTERNAL_SERVER_ERROR",
                        "Unexpected error occurred",
                        request.getRequestURI()
                ));
    }
}
