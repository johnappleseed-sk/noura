package com.noura.catalog.exception;

import com.noura.catalog.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Maps not-found failures to HTTP 404.
     *
     * @param ex not-found exception
     * @param request current HTTP request
     * @return standard not-found envelope
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("Resource not found", ex.getCode(), ex.getMessage(), request.getRequestURI()));
    }

    /**
     * Maps request body validation failures to HTTP 400.
     *
     * @param ex validation exception
     * @param request current HTTP request
     * @return standard validation envelope
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
     * @param ex validation exception
     * @param request current HTTP request
     * @return standard validation envelope
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
     * Maps malformed request bodies to HTTP 400.
     *
     * @param ex body parse exception
     * @param request current HTTP request
     * @return standard validation envelope
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(
                        "Validation failed",
                        "INVALID_REQUEST_BODY",
                        "Request body is malformed or contains unsupported values",
                        request.getRequestURI()
                ));
    }

    /**
     * Maps path/query type mismatch failures to HTTP 400.
     *
     * @param ex type mismatch exception
     * @param request current HTTP request
     * @return standard validation envelope
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(
                        "Validation failed",
                        "VALIDATION_ERROR",
                        "Invalid value for parameter '" + ex.getName() + "'",
                        request.getRequestURI()
                ));
    }

    /**
     * Fallback mapping for unexpected failures.
     *
     * @param ex unexpected exception
     * @param request current HTTP request
     * @return internal server error envelope
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(
                        "Internal server error",
                        "INTERNAL_SERVER_ERROR",
                        "Unexpected error occurred",
                        request.getRequestURI()
                ));
    }
}
