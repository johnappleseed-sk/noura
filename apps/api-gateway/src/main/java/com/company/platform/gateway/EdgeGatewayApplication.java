package com.company.platform.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@SpringBootApplication
public class EdgeGatewayApplication {

    private static final Logger log = LoggerFactory.getLogger(EdgeGatewayApplication.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(EdgeGatewayApplication.class, args);
        logStartup(context.getEnvironment());
    }

    /**
     * Logs the gateway's operationally relevant startup summary for local/dev debugging.
     *
     * @param environment Spring environment
     */
    private static void logStartup(Environment environment) {
        log.info(
                "service_startup service={} port={} health={} readiness={} liveness={} serviceResolution={} authEnabled={} profiles={}",
                environment.getProperty("spring.application.name", "edge-gateway"),
                environment.getProperty("local.server.port", environment.getProperty("server.port", "8080")),
                "/actuator/health",
                "/actuator/health/readiness",
                "/actuator/health/liveness",
                resolveServiceResolutionMode(environment),
                environment.getProperty("gateway.auth.enabled", "false"),
                resolveProfiles(environment)
        );
    }

    /**
     * Resolves how the gateway expects to reach downstream services for the current runtime.
     *
     * @param environment Spring environment
     * @return {@code discovery} when registry-based routing is enabled, otherwise {@code explicit-uris}
     */
    static String resolveServiceResolutionMode(Environment environment) {
        boolean discoveryEnabled = environment.getProperty("spring.cloud.discovery.enabled", Boolean.class, false);
        boolean discoveryLocatorEnabled = environment.getProperty(
                "spring.cloud.gateway.discovery.locator.enabled",
                Boolean.class,
                false
        );
        return (discoveryEnabled || discoveryLocatorEnabled) ? "discovery" : "explicit-uris";
    }

    /**
     * Resolves active profiles into one log-friendly value.
     *
     * @param environment Spring environment
     * @return comma-separated profiles or {@code default}
     */
    private static String resolveProfiles(Environment environment) {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return "default";
        }
        return String.join(",", Arrays.asList(activeProfiles));
    }
}
