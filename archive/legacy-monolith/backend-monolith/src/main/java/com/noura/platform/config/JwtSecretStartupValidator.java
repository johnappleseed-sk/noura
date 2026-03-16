package com.noura.platform.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtSecretStartupValidator {

    private static final int MIN_SECRET_LENGTH = 32;
    private static final Set<String> DISALLOWED_SECRET_VALUES = Set.of(
            "changeme",
            "change-me",
            "default",
            "default-secret",
            "default-jwt-secret",
            "jwt-secret",
            "secret",
            "your-256-bit-secret",
            "replace-me"
    );

    private final AppProperties appProperties;
    private final Environment environment;

    @PostConstruct
    void validateJwtSecret() {
        if (!requiresStrictValidation()) {
            return;
        }
        String secret = appProperties.getJwt().getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret must be configured in non-local environments");
        }
        if (secret.trim().length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("JWT secret must be at least 32 characters in non-local environments");
        }
        if (isWeakOrDefaultSecret(secret)) {
            throw new IllegalStateException("JWT secret appears to be default/weak and is not allowed in non-local environments");
        }
    }

    boolean requiresStrictValidation() {
        return StartupValidationProfiles.requiresStrictValidation(environment);
    }

    boolean isWeakOrDefaultSecret(String secret) {
        if (secret == null) {
            return true;
        }
        String normalized = secret.trim().toLowerCase(Locale.ROOT);
        if (DISALLOWED_SECRET_VALUES.contains(normalized)) {
            return true;
        }
        if (normalized.contains("changeme")
                || normalized.contains("change-me")
                || normalized.contains("default")
                || normalized.contains("replace-me")) {
            return true;
        }
        return normalized.matches("^(.)\\1{31,}$");
    }
}
