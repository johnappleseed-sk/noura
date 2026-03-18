package com.noura.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Order Service Spring Boot application.
 */
@SpringBootApplication
public class OrderServiceApplication {

    /**
     * Boots the Order Service process.
     *
     * @param args JVM startup arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}

