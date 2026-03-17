package com.noura.review.controller.support;

import com.noura.review.config.InternalApiProperties;
import com.noura.review.service.model.ReviewRequestContext;
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
public class ReviewRequestContextResolver {

    private static final String SUBJECT_HEADER = "X-Auth-Subject";
    private static final String USERNAME_HEADER = "X-Auth-Username";
    private static final String ROLES_HEADER = "X-Auth-Roles";
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String USER_AGENT_HEADER = "User-Agent";

    private final InternalApiProperties internalApiProperties;

    /**
     * Resolves actor context from request headers.
     *
     * @param request current HTTP request
     * @return resolved actor context
     */
    public ReviewRequestContext resolve(HttpServletRequest request) {
        String subject = normalizeNullable(request.getHeader(SUBJECT_HEADER));
        String username = normalizeNullable(request.getHeader(USERNAME_HEADER));
        String authorization = normalizeNullable(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (subject == null) {
            subject = resolveAuthorizationFingerprint(authorization);
        }
        Set<String> roles = parseRoles(request.getHeader(ROLES_HEADER));
        boolean internalCall = matchesInternalApiKey(request.getHeader(INTERNAL_API_KEY_HEADER));
        return new ReviewRequestContext(
                subject,
                username,
                authorization,
                roles,
                internalCall,
                resolveRemoteAddress(request),
                normalizeNullable(request.getHeader(USER_AGENT_HEADER))
        );
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
     * Validates the internal API key header against the configured key.
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
     * Resolves a stable fallback subject from bearer token content.
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
     * Resolves client IP from forwarded headers before falling back to servlet remote address.
     *
     * @param request current request
     * @return normalized remote address or {@code null}
     */
    private String resolveRemoteAddress(HttpServletRequest request) {
        String forwardedFor = normalizeNullable(request.getHeader(FORWARDED_FOR_HEADER));
        if (forwardedFor != null) {
            String first = Arrays.stream(forwardedFor.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .findFirst()
                    .orElse(null);
            if (first != null) {
                return first;
            }
        }
        return normalizeNullable(request.getRemoteAddr());
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
