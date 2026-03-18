package com.noura.customer.controller.support;

import com.noura.customer.exception.CustomerOperationException;
import com.noura.customer.service.model.CustomerIdentity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Resolves customer identity context from gateway-forwarded headers or bearer token fallback.
 *
 * <p>The resolver keeps authentication assumptions minimal:
 * it prefers {@code X-Auth-Subject} and can derive a stable fallback subject
 * from a bearer token fingerprint when the gateway is not forwarding claims.</p>
 */
@Component
public class CustomerIdentityResolver {

    private static final String SUBJECT_HEADER = "X-Auth-Subject";
    private static final String EMAIL_HEADER = "X-Auth-Email";
    private static final String USERNAME_HEADER = "X-Auth-Username";

    /**
     * Resolves required customer identity for account endpoints.
     *
     * @param request current HTTP request
     * @return resolved customer identity
     * @throws CustomerOperationException when no identity information is present
     */
    public CustomerIdentity resolveRequiredIdentity(HttpServletRequest request) {
        String externalSubject = trimToNull(request.getHeader(SUBJECT_HEADER));
        String emailHint = resolveEmailHint(request);

        if (externalSubject != null) {
            return new CustomerIdentity(externalSubject, emailHint);
        }

        String bearerToken = extractBearerToken(trimToNull(request.getHeader(HttpHeaders.AUTHORIZATION)));
        if (bearerToken != null) {
            return new CustomerIdentity("token:" + fingerprint(bearerToken), emailHint);
        }

        throw new CustomerOperationException(
                HttpStatus.UNAUTHORIZED,
                "AUTH_SUBJECT_REQUIRED",
                "Authenticated customer identity is required"
        );
    }

    /**
     * Resolves optional email hint from forwarded headers.
     *
     * @param request current HTTP request
     * @return normalized email hint or {@code null}
     */
    private String resolveEmailHint(HttpServletRequest request) {
        String explicitEmail = trimToNull(request.getHeader(EMAIL_HEADER));
        if (explicitEmail != null) {
            return explicitEmail.toLowerCase(Locale.ROOT);
        }

        String forwardedUsername = trimToNull(request.getHeader(USERNAME_HEADER));
        if (forwardedUsername != null && forwardedUsername.contains("@")) {
            return forwardedUsername.toLowerCase(Locale.ROOT);
        }
        return null;
    }

    /**
     * Extracts bearer token value from {@code Authorization} header.
     *
     * @param authorizationHeader raw authorization header
     * @return bearer token or {@code null} when missing/invalid
     */
    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null) {
            return null;
        }
        if (!authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return trimToNull(authorizationHeader.substring(7));
    }

    /**
     * Derives a short, stable SHA-256 fingerprint for bearer tokens.
     *
     * @param token bearer token string
     * @return hex fingerprint prefix
     */
    private String fingerprint(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 12);
        } catch (NoSuchAlgorithmException ex) {
            throw new CustomerOperationException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "TOKEN_FINGERPRINT_UNAVAILABLE",
                    "Unable to derive token fingerprint"
            );
        }
    }

    /**
     * Trims source text and normalizes blanks to {@code null}.
     *
     * @param value source text
     * @return normalized value or {@code null}
     */
    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

