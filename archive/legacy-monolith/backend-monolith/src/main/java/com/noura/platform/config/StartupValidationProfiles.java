package com.noura.platform.config;

import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

public final class StartupValidationProfiles {

    private static final Set<String> LOCAL_PROFILES = Set.of(
            "local",
            "local-postgres",
            "local-mysql",
            "docker",
            "dev",
            "test"
    );

    private StartupValidationProfiles() {
    }

    public static boolean requiresStrictValidation(Environment environment) {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return false;
        }
        return Arrays.stream(activeProfiles)
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .anyMatch(profile -> !LOCAL_PROFILES.contains(profile));
    }
}
