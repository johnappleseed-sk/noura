package com.noura.shipping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the shipping-service.
 */
@SpringBootApplication
public class ShippingServiceApplication {

    /**
     * Launches the service.
     *
     * @param args process arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ShippingServiceApplication.class, args);
    }
}
