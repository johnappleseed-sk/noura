package com.noura.platform.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SchemaSafetyStartupValidator {

    private static final Logger log = LoggerFactory.getLogger(SchemaSafetyStartupValidator.class);
    private static final Set<String> UNSAFE_DDL_MODES = Set.of("update", "create", "create-drop");
    private static final Set<String> STRICT_ALLOWED_DDL_MODES = Set.of("validate", "none");
    private static final String FLYWAY_ENABLED = "spring.flyway.enabled";
    private static final String FLYWAY_VALIDATE_ON_MIGRATE = "spring.flyway.validate-on-migrate";
    private static final String FLYWAY_CLEAN_DISABLED = "spring.flyway.clean-disabled";
    private static final String FLYWAY_OUT_OF_ORDER = "spring.flyway.out-of-order";
    private static final String FLYWAY_FAIL_ON_MISSING_LOCATIONS = "spring.flyway.fail-on-missing-locations";
    private static final String FLYWAY_LOCATIONS = "spring.flyway.locations";
    private static final String REQUIRED_FLYWAY_LOCATION = "classpath:db/migration";

    private final Environment environment;

    @PostConstruct
    void validateSchemaSafety() {
        String ddlMode = normalize(environment.getProperty("spring.jpa.hibernate.ddl-auto"));
        boolean flywayEnabled = environment.getProperty(FLYWAY_ENABLED, Boolean.class, true);
        boolean flywayValidateOnMigrate = environment.getProperty(FLYWAY_VALIDATE_ON_MIGRATE, Boolean.class, true);
        boolean flywayCleanDisabled = environment.getProperty(FLYWAY_CLEAN_DISABLED, Boolean.class, true);
        boolean flywayOutOfOrder = environment.getProperty(FLYWAY_OUT_OF_ORDER, Boolean.class, false);
        boolean flywayFailOnMissingLocations = environment.getProperty(FLYWAY_FAIL_ON_MISSING_LOCATIONS, Boolean.class, true);
        String flywayLocations = environment.getProperty(FLYWAY_LOCATIONS, REQUIRED_FLYWAY_LOCATION);
        boolean inventoryEnabled = environment.getProperty("inventory.enabled", Boolean.class, false);
        String inventoryDatasourceUrl = environment.getProperty("inventory.datasource.url");
        boolean remoteNotificationsEnabled = environment.getProperty("app.notifications.remote.enabled", Boolean.class, false);
        String remoteNotificationsBaseUrl = environment.getProperty("app.notifications.remote.base-url");
        boolean remoteNotificationsFallbackToLocal = environment.getProperty(
                "app.notifications.remote.fallback-to-local",
                Boolean.class,
                true
        );

        if (inventoryEnabled && (inventoryDatasourceUrl == null || inventoryDatasourceUrl.isBlank())) {
            throw new IllegalStateException(
                    "Inventory module misconfiguration: inventory.enabled=true requires 'inventory.datasource.url' to be configured."
            );
        }
        if (remoteNotificationsEnabled
                && !remoteNotificationsFallbackToLocal
                && (remoteNotificationsBaseUrl == null || remoteNotificationsBaseUrl.isBlank())) {
            throw new IllegalStateException(
                    "Notification module misconfiguration: app.notifications.remote.enabled=true with fallback disabled"
                            + " requires 'app.notifications.remote.base-url' to be configured."
            );
        }

        if (!requiresStrictValidation()) {
            if (UNSAFE_DDL_MODES.contains(ddlMode)) {
                log.warn("Schema safety warning: running with ddl-auto={} profile={}." +
                                " This mode is intended for local development only.",
                        ddlMode, String.join(",", environment.getActiveProfiles()));
            }
            if (!flywayCleanDisabled) {
                log.warn("Flyway safety warning: clean-disabled=false on profile={}.", String.join(",", environment.getActiveProfiles()));
            }
            if (remoteNotificationsEnabled && (remoteNotificationsBaseUrl == null || remoteNotificationsBaseUrl.isBlank())) {
                log.warn("Notification remote bridge enabled without base-url. Local fallback path will be used.");
            }
            return;
        }

        if (UNSAFE_DDL_MODES.contains(ddlMode)) {
            throw new IllegalStateException(
                    "Unsafe schema mode '" + ddlMode + "' is not allowed in non-local profiles. Use migration-first schema management."
            );
        }
        if (!ddlMode.isBlank() && !STRICT_ALLOWED_DDL_MODES.contains(ddlMode)) {
            throw new IllegalStateException(
                    "Unsupported ddl-auto mode '" + ddlMode + "' in non-local profiles. Use 'validate' (or 'none' when externally validated)."
            );
        }
        if (!flywayEnabled) {
            throw new IllegalStateException("Flyway must be enabled in non-local profiles.");
        }
        if (!flywayValidateOnMigrate) {
            throw new IllegalStateException("Flyway validate-on-migrate must be enabled in non-local profiles.");
        }
        if (!flywayFailOnMissingLocations) {
            throw new IllegalStateException("Flyway fail-on-missing-locations must be enabled in non-local profiles.");
        }
        if (!flywayCleanDisabled) {
            throw new IllegalStateException("Flyway clean must remain disabled in non-local profiles.");
        }
        if (flywayOutOfOrder) {
            throw new IllegalStateException("Flyway out-of-order migrations are not allowed in non-local profiles.");
        }
        if (!containsRequiredFlywayLocation(flywayLocations)) {
            throw new IllegalStateException(
                    "Flyway locations must include '" + REQUIRED_FLYWAY_LOCATION + "' in non-local profiles."
            );
        }
    }

    boolean requiresStrictValidation() {
        return StartupValidationProfiles.requiresStrictValidation(environment);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean containsRequiredFlywayLocation(String locations) {
        if (locations == null || locations.isBlank()) {
            return false;
        }
        String[] tokens = locations.split(",");
        for (String token : tokens) {
            if (normalize(token).equals(REQUIRED_FLYWAY_LOCATION)) {
                return true;
            }
        }
        return false;
    }
}
