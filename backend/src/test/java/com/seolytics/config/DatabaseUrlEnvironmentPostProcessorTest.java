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
    void convertsRenderPostgresUrlAndUsesEmbeddedCredentials() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("DATABASE_URL", "postgresql://render-user:render-password@render-host:5432/seolytics?sslmode=require");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://render-host:5432/seolytics?sslmode=require");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("render-user");
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("render-password");
    }

    @Test
    void keepsExplicitDatasourceCredentialsOverUrlCredentials() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("DATABASE_URL", "postgres://url-user:url-password@render-host:5432/seolytics")
                .withProperty("SPRING_DATASOURCE_USERNAME", "configured-user")
                .withProperty("SPRING_DATASOURCE_PASSWORD", "configured-password");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://render-host:5432/seolytics");
        assertThat(environment.getProperty("spring.datasource.username")).isNull();
        assertThat(environment.getProperty("spring.datasource.password")).isNull();
        assertThat(environment.getProperty("SPRING_DATASOURCE_USERNAME")).isEqualTo("configured-user");
        assertThat(environment.getProperty("SPRING_DATASOURCE_PASSWORD")).isEqualTo("configured-password");
    }

    @Test
    void leavesJdbcUrlUnchanged() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("DATABASE_URL", "jdbc:mysql://render-host:3306/seolytics");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url")).isNull();
    }
}
