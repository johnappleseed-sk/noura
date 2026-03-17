package com.noura.review;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the review-service.
 */
@SpringBootApplication
public class ReviewServiceApplication {

    /**
     * Launches the service.
     *
     * @param args process arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ReviewServiceApplication.class, args);
    }
}
