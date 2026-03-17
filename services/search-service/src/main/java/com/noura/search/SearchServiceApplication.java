package com.noura.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the search-service.
 */
@SpringBootApplication
public class SearchServiceApplication {

    /**
     * Launches the service.
     *
     * @param args process arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(SearchServiceApplication.class, args);
    }
}
