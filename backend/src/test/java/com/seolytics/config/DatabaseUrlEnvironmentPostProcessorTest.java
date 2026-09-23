package com.seolytics.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();

    @Test
    void convertsMysqlDatabaseUrlToJdbcUrl() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("DATABASE_URL", "mysql://render-host:3306/seolytics");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:mysql://render-host:3306/seolytics");
    }

    @Test
    void keepsUsernameAndPasswordEnvironmentBased() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("DATABASE_URL", "mysql://url-user:url-pass@render-host:3306/seolytics")
                .withProperty("DATABASE_USERNAME", "render-user")
                .withProperty("DATABASE_PASSWORD", "render-password");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:mysql://render-host:3306/seolytics");
        assertThat(environment.getProperty("spring.datasource.username")).isNull();
        assertThat(environment.getProperty("spring.datasource.password")).isNull();
        assertThat(environment.getProperty("DATABASE_USERNAME")).isEqualTo("render-user");
        assertThat(environment.getProperty("DATABASE_PASSWORD")).isEqualTo("render-password");
    }

    @Test
    void leavesJdbcUrlUnchanged() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("DATABASE_URL", "jdbc:mysql://render-host:3306/seolytics");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url")).isNull();
    }
}
