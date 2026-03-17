package com.noura.promotion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the promotion-service.
 */
@SpringBootApplication
public class PromotionServiceApplication {

    /**
     * Launches the service.
     *
     * @param args process arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(PromotionServiceApplication.class, args);
    }
}
