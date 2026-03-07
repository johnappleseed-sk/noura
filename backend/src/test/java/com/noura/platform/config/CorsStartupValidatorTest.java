package com.noura.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CorsStartupValidatorTest {

    @Test
    void validateCorsOrigins_shouldFailWhenMissingInNonLocalProfile() {
        AppProperties appProperties = new AppProperties();
        appProperties.getCors().setAllowedOrigins("");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("staging");

        CorsStartupValidator validator = new CorsStartupValidator(appProperties, environment);

        assertThrows(IllegalStateException.class, validator::validateCorsOrigins);
    }

    @Test
    void validateCorsOrigins_shouldFailWhenWildcardUsedInNonLocalProfile() {
        AppProperties appProperties = new AppProperties();
        appProperties.getCors().setAllowedOrigins("*");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        CorsStartupValidator validator = new CorsStartupValidator(appProperties, environment);

        assertThrows(IllegalStateException.class, validator::validateCorsOrigins);
    }

    @Test
    void validateCorsOrigins_shouldAllowConfiguredOriginsInNonLocalProfile() {
        AppProperties appProperties = new AppProperties();
        appProperties.getCors().setAllowedOrigins("https://admin.noura.com,https://ops.noura.com");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        CorsStartupValidator validator = new CorsStartupValidator(appProperties, environment);

        assertDoesNotThrow(validator::validateCorsOrigins);
    }

    @Test
    void validateCorsOrigins_shouldAllowEmptyOriginsInLocalProfile() {
        AppProperties appProperties = new AppProperties();
        appProperties.getCors().setAllowedOrigins("");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");

        CorsStartupValidator validator = new CorsStartupValidator(appProperties, environment);

        assertDoesNotThrow(validator::validateCorsOrigins);
    }
}
