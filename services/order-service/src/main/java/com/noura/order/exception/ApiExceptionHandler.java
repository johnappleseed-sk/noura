package com.noura.order.exception;

import com.noura.order.common.ApiResponse;
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
 * Global exception-to-response mapper for Order Service APIs.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Maps body validation failures to HTTP 400.
     *
     * @param ex body validation exception
     * @param request current request
     * @return standardized validation error response
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
     * @param ex query/path validation exception
     * @param request current request
     * @return standardized validation error response
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
     * Maps type mismatch failures to HTTP 400.
     *
     * @param ex argument type mismatch exception
     * @param request current request
     * @return standardized validation error response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(
                "Validation failed",
                "VALIDATION_ERROR",
                "Invalid value for parameter '" + ex.getName() + "'",
                request.getRequestURI()
        ));
    }

    /**
     * Maps not-found errors to HTTP 404.
     *
     * @param ex not-found exception
     * @param request current request
     * @return standardized not-found response
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
     * Maps order operation errors to their configured HTTP status.
     *
     * @param ex operation exception
     * @param request current request
     * @return standardized operation failure response
     */
    @ExceptionHandler(OrderOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleOperation(OrderOperationException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.fail(
                "Order operation rejected",
                ex.getCode(),
                ex.getMessage(),
                request.getRequestURI()
        ));
    }

    /**
     * Fallback mapper for unexpected server errors.
     *
     * @param ex unexpected exception
     * @param request current request
     * @return standardized internal server error response
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

