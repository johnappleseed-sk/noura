package com.noura.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtSecretStartupValidatorTest {

    @Test
    void validateJwtSecret_shouldFailWhenMissingInNonLocalProfile() {
        AppProperties appProperties = new AppProperties();
        appProperties.getJwt().setSecret("");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("staging");

        JwtSecretStartupValidator validator = new JwtSecretStartupValidator(appProperties, environment);

        assertThrows(IllegalStateException.class, validator::validateJwtSecret);
    }

    @Test
    void validateJwtSecret_shouldFailWhenTooShortInNonLocalProfile() {
        AppProperties appProperties = new AppProperties();
        appProperties.getJwt().setSecret("short-secret");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        JwtSecretStartupValidator validator = new JwtSecretStartupValidator(appProperties, environment);

        assertThrows(IllegalStateException.class, validator::validateJwtSecret);
    }

    @Test
    void validateJwtSecret_shouldAllowShortSecretInLocalProfile() {
        AppProperties appProperties = new AppProperties();
        appProperties.getJwt().setSecret("short-secret");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");

        JwtSecretStartupValidator validator = new JwtSecretStartupValidator(appProperties, environment);

        assertDoesNotThrow(validator::validateJwtSecret);
    }

    @Test
    void validateJwtSecret_shouldAllowStrongSecretInNonLocalProfile() {
        AppProperties appProperties = new AppProperties();
        appProperties.getJwt().setSecret("0123456789abcdef0123456789abcdef");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        JwtSecretStartupValidator validator = new JwtSecretStartupValidator(appProperties, environment);

        assertDoesNotThrow(validator::validateJwtSecret);
    }

    @Test
    void validateJwtSecret_shouldFailWhenDefaultLikeSecretUsedInNonLocalProfile() {
        AppProperties appProperties = new AppProperties();
        appProperties.getJwt().setSecret("change-me-change-me-change-me-change-me");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("staging");

        JwtSecretStartupValidator validator = new JwtSecretStartupValidator(appProperties, environment);

        assertThrows(IllegalStateException.class, validator::validateJwtSecret);
    }

    @Test
    void validateJwtSecret_shouldFailWhenLowEntropyRepeatedSecretUsedInNonLocalProfile() {
        AppProperties appProperties = new AppProperties();
        appProperties.getJwt().setSecret("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        JwtSecretStartupValidator validator = new JwtSecretStartupValidator(appProperties, environment);

        assertThrows(IllegalStateException.class, validator::validateJwtSecret);
    }
}
