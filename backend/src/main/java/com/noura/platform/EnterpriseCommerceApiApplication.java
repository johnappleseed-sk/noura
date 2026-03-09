package com.noura.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableCaching
@EntityScan(basePackages = "com.noura.platform.domain.entity")
@EnableJpaRepositories(basePackages = "com.noura.platform.repository")
@ComponentScan(
        basePackages = "com.noura.platform",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.noura\\.platform\\.commerce\\..*"
        )
)
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
