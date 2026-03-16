package com.noura.platform.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CorsStartupValidator {

    private final AppProperties appProperties;
    private final Environment environment;

    @PostConstruct
    void validateCorsOrigins() {
        if (!requiresStrictValidation()) {
            return;
        }
        List<String> origins = parseAllowedOrigins();
        if (origins.isEmpty()) {
            throw new IllegalStateException("CORS allowed origins must be configured in non-local environments");
        }
        boolean hasWildcard = origins.stream().anyMatch(origin -> origin.contains("*"));
        if (hasWildcard) {
            throw new IllegalStateException("Wildcard CORS origins are not allowed in non-local environments");
        }
    }

    boolean requiresStrictValidation() {
        return StartupValidationProfiles.requiresStrictValidation(environment);
    }

    private List<String> parseAllowedOrigins() {
        return Arrays.stream(Optional.ofNullable(appProperties.getCors().getAllowedOrigins()).orElse("").split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }
}
