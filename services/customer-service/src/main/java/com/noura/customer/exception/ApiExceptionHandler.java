package com.noura.customer.exception;

import com.noura.customer.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception mapper for Customer Service APIs.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Maps body validation failures to HTTP 400.
     *
     * @param ex method argument validation exception
     * @param request current request
     * @return validation error response envelope
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(ApiResponse.fail(
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
     * @param ex constraint validation exception
     * @param request current request
     * @return validation error response envelope
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(
                "Validation failed",
                "VALIDATION_ERROR",
                ex.getMessage(),
                request.getRequestURI()
        ));
    }

    /**
     * Maps path/query type mismatch failures to HTTP 400.
     *
     * @param ex type mismatch exception
     * @param request current request
     * @return validation error response envelope
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String detail = "Invalid value for parameter '" + ex.getName() + "'";
        return ResponseEntity.badRequest().body(ApiResponse.fail(
                "Validation failed",
                "VALIDATION_ERROR",
                detail,
                request.getRequestURI()
        ));
    }

    /**
     * Maps not-found exceptions to HTTP 404.
     *
     * @param ex not-found exception
     * @param request current request
     * @return not-found error envelope
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(404).body(ApiResponse.fail(
                "Resource not found",
                ex.getCode(),
                ex.getMessage(),
                request.getRequestURI()
        ));
    }

    /**
     * Maps operation exceptions to configured HTTP status.
     *
     * @param ex operation exception
     * @param request current request
     * @return operation error envelope
     */
    @ExceptionHandler(CustomerOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleOperation(CustomerOperationException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.fail(
                "Customer operation rejected",
                ex.getCode(),
                ex.getMessage(),
                request.getRequestURI()
        ));
    }

    /**
     * Maps unexpected errors to HTTP 500.
     *
     * @param ex unexpected exception
     * @param request current request
     * @return generic error envelope
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(500).body(ApiResponse.fail(
                "Internal server error",
                "INTERNAL_SERVER_ERROR",
                "Unexpected error occurred",
                request.getRequestURI()
        ));
    }
}
