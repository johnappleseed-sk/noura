package com.noura.platform.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;

/**
 * The monolith uses multiple persistence units (default + inventory).
 * When any custom EntityManagerFactory is present, Spring Boot backs off from
 * creating the default one, so we define it explicitly here.
 */
@Configuration
public class PlatformPersistenceConfig {

    /**
     * Spring Boot auto-config backs off when *any* {@link javax.sql.DataSource} bean exists. Since the inventory
     * module defines its own DataSource, we must declare the platform DataSource explicitly to ensure the default
     * persistence unit uses {@code spring.datasource.*} (typically the {@code noura} database) while the inventory
     * persistence unit uses {@code inventory.datasource.*} (typically {@code noura_inventory}).
     */
    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties platformDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean
    public DataSource dataSource(
            @Qualifier("platformDataSourceProperties") DataSourceProperties properties
    ) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Primary
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            DataSource dataSource,
            JpaProperties jpaProperties,
            HibernateProperties hibernateProperties
    ) {
        Map<String, Object> vendorProperties = hibernateProperties.determineHibernateProperties(
                jpaProperties.getProperties(),
                new HibernateSettings()
        );
        return builder
                .dataSource(dataSource)
                .packages("com.noura.platform.domain.entity")
                .persistenceUnit("default")
                .properties(vendorProperties)
                .build();
    }

    @Primary
    @Bean
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
