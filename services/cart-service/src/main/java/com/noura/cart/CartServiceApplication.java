package com.noura.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Cart Service Spring Boot application.
 */
@SpringBootApplication
public class CartServiceApplication {

    /**
     * Boots the Cart Service process.
     *
     * @param args JVM startup arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(CartServiceApplication.class, args);
    }
}
