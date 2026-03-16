package com.noura.pricing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Pricing Service Spring Boot application.
 */
@SpringBootApplication
public class PricingServiceApplication {

    /**
     * Boots the Pricing Service process.
     *
     * @param args JVM startup arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(PricingServiceApplication.class, args);
    }
}

