package com.noura.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Inventory Service Spring Boot application.
 *
 * <p>This module exposes stock visibility and stock mutation APIs used by admin and
 * storefront workflows.</p>
 */
@SpringBootApplication
public class InventoryServiceApplication {

    /**
     * Boots the Inventory Service process.
     *
     * @param args JVM startup arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
