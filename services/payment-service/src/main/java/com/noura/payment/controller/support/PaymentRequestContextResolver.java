package com.noura.payment.controller.support;

import com.noura.payment.config.InternalApiProperties;
import com.noura.payment.service.model.PaymentRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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
import java.util.stream.Collectors;

/**
 * Resolves request-level actor context from gateway-forwarded and internal-service headers.
 */
@Component
@RequiredArgsConstructor
public class PaymentRequestContextResolver {

    private static final String SUBJECT_HEADER = "X-Auth-Subject";
    private static final String ROLES_HEADER = "X-Auth-Roles";
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final InternalApiProperties internalApiProperties;

    /**
     * Resolves actor context from request headers.
     *
     * @param request current HTTP request
     * @return resolved actor context
     */
    public PaymentRequestContext resolve(HttpServletRequest request) {
        String subject = normalizeNullable(request.getHeader(SUBJECT_HEADER));
        String authorization = normalizeNullable(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (subject == null) {
            subject = resolveAuthorizationFingerprint(authorization);
        }
        Set<String> roles = parseRoles(request.getHeader(ROLES_HEADER));
        boolean internalCall = matchesInternalApiKey(request.getHeader(INTERNAL_API_KEY_HEADER));
        return new PaymentRequestContext(subject, authorization, roles, internalCall);
    }

    /**
     * Parses role header into a normalized role set.
     *
     * @param rawRoles roles header value
     * @return role set
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
     * Validates internal API key header against configured key.
     *
     * @param providedApiKey provided key
     * @return {@code true} when valid
     */
    private boolean matchesInternalApiKey(String providedApiKey) {
        String configuredApiKey = normalizeNullable(internalApiProperties.getApiKey());
        if (configuredApiKey == null) {
            return false;
        }
        return configuredApiKey.equals(normalizeNullable(providedApiKey));
    }

    /**
     * Resolves stable fallback subject from bearer token content.
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
     * Trims text and normalizes blanks to null.
     *
     * @param value source text
     * @return normalized text or null
     */
    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
