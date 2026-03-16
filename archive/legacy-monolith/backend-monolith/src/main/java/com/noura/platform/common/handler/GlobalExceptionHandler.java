package com.noura.platform.common.handler;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.MDC;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
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
        return buildError(
                ex.getStatus(),
                ex.getMessage(),
                ex.getCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
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
        Map<String, String> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        this::resolveFieldMessage,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(
                        "Validation failed",
                        "VALIDATION_ERROR",
                        "One or more validation errors occurred",
                        validationErrors,
                        request.getRequestURI()
                ));
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
        Map<String, String> validationErrors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage() == null ? "Invalid value" : violation.getMessage(),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(
                            "Validation failed",
                            "VALIDATION_ERROR",
                            "One or more validation errors occurred",
                            validationErrors,
                            request.getRequestURI()
                    ));
        }
        return buildError(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "VALIDATION_ERROR",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.CONFLICT,
                "Data conflict",
                "DATA_CONFLICT",
                "The requested change violates a uniqueness or relationship constraint",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String detail = ex.getName() + " has invalid value";
        return buildError(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "VALIDATION_ERROR",
                detail,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingPart(
            MissingServletRequestPartException ex,
            HttpServletRequest request
    ) {
        String detail = "Missing required request part: " + ex.getRequestPartName();
        return buildError(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "VALIDATION_ERROR",
                detail,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request
    ) {
        String detail = "Unsupported media type: " + ex.getContentType();
        return buildError(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported media type",
                "UNSUPPORTED_MEDIA_TYPE",
                detail,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        String detail = "Missing required parameter: " + ex.getParameterName();
        return buildError(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "VALIDATION_ERROR",
                detail,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "Invalid request body",
                "REQUEST_BODY_INVALID",
                "Request payload is malformed or contains invalid values",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotWritable(HttpMessageNotWritableException ex, HttpServletRequest request) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "Response serialization failed",
                "RESPONSE_SERIALIZATION_FAILED",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "Bad request",
                "BAD_REQUEST",
                ex.getMessage(),
                request.getRequestURI()
        );
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
        return buildError(
                HttpStatus.FORBIDDEN,
                "Access denied",
                "ACCESS_DENIED",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        String method = ex.getMethod();
        String detail = method == null ? "HTTP method is not supported for this endpoint" :
                "HTTP method " + method + " is not supported for this endpoint";
        return buildError(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method not allowed",
                "METHOD_NOT_ALLOWED",
                detail,
                request.getRequestURI()
        );
    }

    /**
     * Executes handle not found.
     *
     * @param ex The ex value.
     * @param request The request payload for this operation.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception ex, HttpServletRequest request) {
        return buildError(
                HttpStatus.NOT_FOUND,
                "Endpoint not found",
                "NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        );
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
        log.error(
                "Unhandled error on {} {} [correlationId={}]",
                request.getMethod(),
                request.getRequestURI(),
                MDC.get("correlationId"),
                ex
        );
        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected server error",
                "INTERNAL_ERROR",
                "Please contact support",
                request.getRequestURI()
        );
    }

    /**
     * Executes format field error.
     *
     * @param fieldError The field error value.
     * @return The result of format field error.
     */
    private String resolveFieldMessage(FieldError fieldError) {
        return fieldError.getDefaultMessage() == null ? "Invalid value" : fieldError.getDefaultMessage();
    }

    private ResponseEntity<ApiResponse<Void>> buildError(
            HttpStatus status,
            String message,
            String code,
            String detail,
            String path
    ) {
        return ResponseEntity.status(status)
                .body(ApiResponse.fail(message, code, detail, path));
    }
}
