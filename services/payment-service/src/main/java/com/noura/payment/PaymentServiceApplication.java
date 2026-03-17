package com.noura.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Payment Service Spring Boot application.
 */
@SpringBootApplication
public class PaymentServiceApplication {

    /**
     * Boots the Payment Service process.
     *
     * @param args JVM startup arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
