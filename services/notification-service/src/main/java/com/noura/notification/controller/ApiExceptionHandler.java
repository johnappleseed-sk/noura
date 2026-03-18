package com.noura.notification.controller;

import com.noura.notification.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(
                        "Validation failed",
                        "VALIDATION_ERROR",
                        "One or more request fields are invalid",
                        validationErrors,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail("Validation failed", "VALIDATION_ERROR", exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(
            HttpMessageNotReadableException exception,
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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(
                        "Validation failed",
                        "VALIDATION_ERROR",
                        "Invalid value for parameter '" + exception.getName() + "'",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleStatus(ResponseStatusException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        HttpStatus effective = status == null ? HttpStatus.BAD_REQUEST : status;
        String detail = exception.getReason() == null || exception.getReason().isBlank()
                ? effective.getReasonPhrase()
                : exception.getReason();
        return buildStatusFailure(effective, resolveStatusCode(effective), detail, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnhandled(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(
                        "Internal server error",
                        "INTERNAL_SERVER_ERROR",
                        "Unexpected error occurred",
                        request.getRequestURI()
                ));
    }

    /**
     * Builds a standardized status-aware failure envelope for authorization and business-rule errors.
     *
     * @param status resolved HTTP status
     * @param code stable machine-readable code
     * @param detail human-readable detail
     * @param request current HTTP request
     * @return failure envelope
     */
    private ResponseEntity<ApiResponse<Void>> buildStatusFailure(
            HttpStatus status,
            String code,
            String detail,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status)
                .body(ApiResponse.fail(
                        resolveStatusMessage(status),
                        code,
                        detail,
                        request.getRequestURI()
                ));
    }

    /**
     * Resolves the client-facing message for one status category.
     *
     * @param status HTTP status
     * @return standardized client-facing message
     */
    private String resolveStatusMessage(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Request rejected";
            case UNAUTHORIZED -> "Unauthorized";
            case FORBIDDEN -> "Forbidden";
            case NOT_FOUND -> "Resource not found";
            case CONFLICT -> "Conflict";
            default -> status.getReasonPhrase();
        };
    }

    /**
     * Resolves a stable machine-readable code for one HTTP status.
     *
     * @param status HTTP status
     * @return stable machine-readable code
     */
    private String resolveStatusCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "REQUEST_REJECTED";
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "FORBIDDEN";
            case NOT_FOUND -> "NOT_FOUND";
            case CONFLICT -> "CONFLICT";
            default -> status.name();
        };
    }
}
