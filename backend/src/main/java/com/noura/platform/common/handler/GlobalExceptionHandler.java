package com.noura.platform.common.handler;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Executes handle api exception.
     *
     * @param ex The ex value.
     * @param request The request payload for this operation.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.fail(ex.getMessage(), ex.getCode(), ex.getMessage(), request.getRequestURI()));
    }

    /**
     * Executes handle validation.
     *
     * @param ex The ex value.
     * @param request The request payload for this operation.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail("Validation failed", "VALIDATION_ERROR", details, request.getRequestURI()));
    }

    /**
     * Executes handle constraint.
     *
     * @param ex The ex value.
     * @param request The request payload for this operation.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail("Validation failed", "VALIDATION_ERROR", ex.getMessage(), request.getRequestURI()));
    }

    /**
     * Executes handle authorization denied.
     *
     * @param ex The ex value.
     * @param request The request payload for this operation.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDenied(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail("Access denied", "ACCESS_DENIED", ex.getMessage(), request.getRequestURI()));
    }

    /**
     * Executes handle unhandled.
     *
     * @param ex The ex value.
     * @param request The request payload for this operation.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnhandled(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("Unexpected server error", "INTERNAL_ERROR", "Please contact support", request.getRequestURI()));
    }

    /**
     * Executes format field error.
     *
     * @param fieldError The field error value.
     * @return The result of format field error.
     */
    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
