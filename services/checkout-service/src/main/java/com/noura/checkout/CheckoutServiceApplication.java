package com.noura.checkout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Checkout Service Spring Boot application.
 */
@SpringBootApplication
public class CheckoutServiceApplication {

    /**
     * Boots the Checkout Service process.
     *
     * @param args JVM startup arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(CheckoutServiceApplication.class, args);
    }
}

