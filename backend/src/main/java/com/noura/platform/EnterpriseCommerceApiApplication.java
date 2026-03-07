package com.noura.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableCaching
@SpringBootApplication
public class EnterpriseCommerceApiApplication {

    /**
     * Executes main.
     *
     * @param args The args value.
     */
    public static void main(String[] args) {
        SpringApplication.run(EnterpriseCommerceApiApplication.class, args);
    }
}
