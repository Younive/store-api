package com.younive.store.common;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
        return flyway;
    }

    // Forces JPA EntityManagerFactory to wait for Flyway bean (mirrors Spring Boot auto-config behavior)
    @Bean
    static FlywayJpaDependencyPostProcessor flywayJpaDependencyPostProcessor() {
        return new FlywayJpaDependencyPostProcessor();
    }

    static class FlywayJpaDependencyPostProcessor extends AbstractDependsOnBeanFactoryPostProcessor {
        FlywayJpaDependencyPostProcessor() {
            super(AbstractEntityManagerFactoryBean.class, "flyway");
        }
    }
}
