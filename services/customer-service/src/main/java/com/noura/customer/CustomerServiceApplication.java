package com.noura.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Customer Service Spring Boot application.
 */
@SpringBootApplication
public class CustomerServiceApplication {

    /**
     * Boots the Customer Service process.
     *
     * @param args JVM startup arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}
