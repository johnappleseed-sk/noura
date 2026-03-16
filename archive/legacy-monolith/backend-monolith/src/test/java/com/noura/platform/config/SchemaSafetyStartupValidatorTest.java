package com.noura.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchemaSafetyStartupValidatorTest {

    @Test
    void validateSchemaSafety_shouldFailWhenUnsafeDdlUsedInNonLocalProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("staging");
        environment.setProperty("spring.jpa.hibernate.ddl-auto", "update");
        environment.setProperty("spring.flyway.enabled", "true");

        SchemaSafetyStartupValidator validator = new SchemaSafetyStartupValidator(environment);

        assertThrows(IllegalStateException.class, validator::validateSchemaSafety);
    }

    @Test
    void validateSchemaSafety_shouldFailWhenFlywayDisabledInNonLocalProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("spring.jpa.hibernate.ddl-auto", "validate");
        environment.setProperty("spring.flyway.enabled", "false");

        SchemaSafetyStartupValidator validator = new SchemaSafetyStartupValidator(environment);

        assertThrows(IllegalStateException.class, validator::validateSchemaSafety);
    }

    @Test
    void validateSchemaSafety_shouldAllowStrictSettingsInNonLocalProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("spring.jpa.hibernate.ddl-auto", "validate");
        environment.setProperty("spring.flyway.enabled", "true");

        SchemaSafetyStartupValidator validator = new SchemaSafetyStartupValidator(environment);

        assertDoesNotThrow(validator::validateSchemaSafety);
    }

    @Test
    void validateSchemaSafety_shouldAllowUnsafeDdlInLocalProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local-postgres");
        environment.setProperty("spring.jpa.hibernate.ddl-auto", "update");
        environment.setProperty("spring.flyway.enabled", "false");

        SchemaSafetyStartupValidator validator = new SchemaSafetyStartupValidator(environment);

        assertDoesNotThrow(validator::validateSchemaSafety);
    }

    @Test
    void validateSchemaSafety_shouldAllowUnsafeDdlInDockerProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("docker");
        environment.setProperty("spring.jpa.hibernate.ddl-auto", "update");
        environment.setProperty("spring.flyway.enabled", "false");

        SchemaSafetyStartupValidator validator = new SchemaSafetyStartupValidator(environment);

        assertDoesNotThrow(validator::validateSchemaSafety);
    }

    @Test
    void validateSchemaSafety_shouldFailWhenInventoryEnabledWithoutDatasourceUrl() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local-postgres");
        environment.setProperty("inventory.enabled", "true");
        environment.setProperty("inventory.datasource.url", "   ");

        SchemaSafetyStartupValidator validator = new SchemaSafetyStartupValidator(environment);

        assertThrows(IllegalStateException.class, validator::validateSchemaSafety);
    }
}
