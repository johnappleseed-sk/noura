package com.company.platform.common.service;

import com.company.platform.common.web.HealthStatusResponse;
import com.company.platform.config.PlatformProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlatformHealthService {

    private final HealthEndpoint healthEndpoint;
    private final PlatformProperties properties;
    private final Environment environment;

    public HealthStatusResponse getHealthStatus() {
        HealthComponent health = healthEndpoint.health();
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());

        return new HealthStatusResponse(
                properties.getName(),
                properties.getEnvironment(),
                health.getStatus().getCode(),
                profiles,
                Instant.now()
        );
    }
}
