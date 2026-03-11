package com.noura.platform.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SchemaSafetyStartupValidator {

    private static final Logger log = LoggerFactory.getLogger(SchemaSafetyStartupValidator.class);
    private static final Set<String> LOCAL_PROFILES = Set.of("local", "local-mysql", "test");
    private static final Set<String> UNSAFE_DDL_MODES = Set.of("update", "create", "create-drop");

    private final Environment environment;

    @PostConstruct
    void validateSchemaSafety() {
        String ddlMode = normalize(environment.getProperty("spring.jpa.hibernate.ddl-auto"));
        boolean flywayEnabled = environment.getProperty("spring.flyway.enabled", Boolean.class, true);

        if (!requiresStrictValidation()) {
            if (UNSAFE_DDL_MODES.contains(ddlMode)) {
                log.warn("Schema safety warning: running with ddl-auto={} profile={}." +
                                " This mode is intended for local development only.",
                        ddlMode, String.join(",", environment.getActiveProfiles()));
            }
            return;
        }

        if (UNSAFE_DDL_MODES.contains(ddlMode)) {
            throw new IllegalStateException(
                    "Unsafe schema mode '" + ddlMode + "' is not allowed in non-local profiles. Use migration-first schema management."
            );
        }
        if (!flywayEnabled) {
            throw new IllegalStateException("Flyway must be enabled in non-local profiles.");
        }
    }

    boolean requiresStrictValidation() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return false;
        }
        return Arrays.stream(activeProfiles)
                .map(this::normalize)
                .anyMatch(profile -> !LOCAL_PROFILES.contains(profile));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
