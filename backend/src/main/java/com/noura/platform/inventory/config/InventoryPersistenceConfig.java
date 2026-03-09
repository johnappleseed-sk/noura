package com.noura.platform.inventory.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "inventory.datasource", name = "url")
@EnableJpaRepositories(
        basePackages = "com.noura.platform.inventory.repository",
        entityManagerFactoryRef = "inventoryEntityManagerFactory",
        transactionManagerRef = "inventoryTransactionManager"
)
public class InventoryPersistenceConfig {

    @Bean
    @ConfigurationProperties("inventory.datasource")
    public DataSourceProperties inventoryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource inventoryDataSource(
            @Qualifier("inventoryDataSourceProperties") DataSourceProperties properties
    ) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    public Flyway inventoryFlyway(@Qualifier("inventoryDataSource") DataSource inventoryDataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(inventoryDataSource)
                .locations("classpath:db/inventory/migration")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
        return flyway;
    }

    @Bean
    @DependsOn("inventoryFlyway")
    public LocalContainerEntityManagerFactoryBean inventoryEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("inventoryDataSource") DataSource inventoryDataSource,
            JpaProperties jpaProperties,
            HibernateProperties hibernateProperties
    ) {
        Map<String, Object> vendorProperties = hibernateProperties.determineHibernateProperties(
                jpaProperties.getProperties(),
                new HibernateSettings()
        );
        // Inventory schema is managed via Flyway migrations (db/inventory/migration). Don't let
        // the monolith's global `spring.jpa.hibernate.ddl-auto` (often `update` in local) mutate it.
        vendorProperties.put("hibernate.hbm2ddl.auto", "none");
        return builder
                .dataSource(inventoryDataSource)
                .packages("com.noura.platform.inventory.domain")
                .persistenceUnit("inventory")
                .properties(vendorProperties)
                .build();
    }

    @Bean
    public PlatformTransactionManager inventoryTransactionManager(
            @Qualifier("inventoryEntityManagerFactory") EntityManagerFactory inventoryEntityManagerFactory
    ) {
        return new JpaTransactionManager(inventoryEntityManagerFactory);
    }
}
