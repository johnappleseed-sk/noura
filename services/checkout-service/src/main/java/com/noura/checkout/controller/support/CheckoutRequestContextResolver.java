package com.noura.checkout.controller.support;

import com.noura.checkout.config.RequestCorrelationFilter;
import com.noura.checkout.service.model.CheckoutRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Resolves checkout request context from gateway-forwarded headers.
 */
@Component
public class CheckoutRequestContextResolver {

    private static final String SUBJECT_HEADER = "X-Auth-Subject";
    private static final String ROLES_HEADER = "X-Auth-Roles";
    private static final String CORRELATION_MDC_KEY = "correlationId";

    /**
     * Resolves request context from request headers.
     *
     * @param request current HTTP request
     * @return resolved checkout request context
     */
    public CheckoutRequestContext resolve(HttpServletRequest request) {
        String subject = normalizeNullable(request.getHeader(SUBJECT_HEADER));
        String authorization = normalizeNullable(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (subject == null) {
            subject = resolveAuthorizationFingerprint(authorization);
        }
        String correlationId = resolveCorrelationId(request.getHeader(RequestCorrelationFilter.HEADER));
        Set<String> roles = parseRoles(request.getHeader(ROLES_HEADER));
        return new CheckoutRequestContext(subject, authorization, correlationId, roles);
    }

    /**
     * Parses role header into a normalized role set.
     *
     * @param rawRoles roles header value
     * @return normalized role set
     */
    private Set<String> parseRoles(String rawRoles) {
        String normalized = normalizeNullable(rawRoles);
        if (normalized == null) {
            return Collections.emptySet();
        }
        return Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    /**
     * Resolves correlation ID using header value, MDC fallback, and random fallback.
     *
     * @param headerValue inbound correlation header value
     * @return normalized correlation ID
     */
    private String resolveCorrelationId(String headerValue) {
        String fromHeader = normalizeNullable(headerValue);
        if (fromHeader != null && fromHeader.length() <= 128) {
            return fromHeader;
        }
        String fromMdc = normalizeNullable(MDC.get(CORRELATION_MDC_KEY));
        if (fromMdc != null && fromMdc.length() <= 128) {
            return fromMdc;
        }
        return UUID.randomUUID().toString();
    }

    /**
     * Resolves stable subject fallback from bearer token by hashing token value.
     *
     * @param authorization normalized authorization header
     * @return token fingerprint subject or {@code null}
     */
    private String resolveAuthorizationFingerprint(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String rawToken = normalizeNullable(authorization.substring(7));
        if (rawToken == null) {
            return null;
        }
        return "bearer-" + sha256(rawToken);
    }

    /**
     * Hashes source text using SHA-256.
     *
     * @param value source text
     * @return lowercase SHA-256 digest
     */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    /**
     * Trims text and normalizes blanks to {@code null}.
     *
     * @param value source text
     * @return normalized text or {@code null}
     */
    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

