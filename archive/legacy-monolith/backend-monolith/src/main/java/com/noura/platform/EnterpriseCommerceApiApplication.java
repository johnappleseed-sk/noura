package com.noura.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Boots the primary Noura Enterprise monolith and enables scheduling for background recovery workers.
 */
@EnableAsync
@EnableScheduling
@EnableCaching
@EntityScan(basePackages = {
        "com.noura.platform.domain.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.noura.platform.repository"
})
@ComponentScan(
        basePackages = {
                "com.noura.platform"
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = {
                        "com\\.noura\\.platform\\.commerce\\..*",
                        "com\\.noura\\.platform\\.inventory\\..*"
                }
        )
)
@ConfigurationPropertiesScan(basePackages = "com.noura.platform.config")
@SpringBootApplication
public class EnterpriseCommerceApiApplication {

    /**
     * Boots the Noura Enterprise API runtime.
     *
     * @param args The command-line arguments.
     */
    public static void main(String[] args) {
        SpringApplication.run(EnterpriseCommerceApiApplication.class, args);
    }
}
