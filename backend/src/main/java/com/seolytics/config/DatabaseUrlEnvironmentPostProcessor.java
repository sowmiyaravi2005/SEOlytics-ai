package com.seolytics.config;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "normalizedDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = firstText(
                environment.getProperty("SPRING_DATASOURCE_URL"),
                environment.getProperty("DATABASE_URL"),
                environment.getProperty("DB_URL"));
        if (!StringUtils.hasText(databaseUrl) || databaseUrl.startsWith("jdbc:")) {
            return;
        }

        NormalizedDatabaseUrl normalized = normalize(databaseUrl);
        if (normalized == null) {
            return;
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.datasource.url", normalized.jdbcUrl());
        if (!hasExplicitUsername(environment) && StringUtils.hasText(normalized.username())) {
            properties.put("spring.datasource.username", normalized.username());
        }
        if (!hasExplicitPassword(environment) && normalized.password() != null) {
            properties.put("spring.datasource.password", normalized.password());
        }

        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    private static NormalizedDatabaseUrl normalize(String databaseUrl) {
        URI uri = URI.create(databaseUrl);
        String jdbcScheme = jdbcScheme(uri.getScheme());
        if (jdbcScheme == null || !StringUtils.hasText(uri.getHost())) {
            return null;
        }

        StringBuilder jdbcUrl = new StringBuilder("jdbc:")
                .append(jdbcScheme)
                .append("://")
                .append(uri.getHost());

        if (uri.getPort() != -1) {
            jdbcUrl.append(':').append(uri.getPort());
        }
        if (StringUtils.hasText(uri.getRawPath())) {
            jdbcUrl.append(uri.getRawPath());
        }
        if (StringUtils.hasText(uri.getRawQuery())) {
            jdbcUrl.append('?').append(uri.getRawQuery());
        }

        String userInfo = uri.getUserInfo();
        if (!StringUtils.hasText(userInfo)) {
            return new NormalizedDatabaseUrl(jdbcUrl.toString(), null, null);
        }

        int passwordSeparator = userInfo.indexOf(':');
        String username = passwordSeparator == -1 ? userInfo : userInfo.substring(0, passwordSeparator);
        String password = passwordSeparator == -1 ? null : userInfo.substring(passwordSeparator + 1);
        return new NormalizedDatabaseUrl(jdbcUrl.toString(), username, password);
    }

    private static String jdbcScheme(String scheme) {
        if ("mysql".equalsIgnoreCase(scheme)) {
            return "mysql";
        }
        if ("postgres".equalsIgnoreCase(scheme) || "postgresql".equalsIgnoreCase(scheme)) {
            return "postgresql";
        }
        return null;
    }

    private static boolean hasExplicitUsername(ConfigurableEnvironment environment) {
        return StringUtils.hasText(firstText(
                environment.getProperty("SPRING_DATASOURCE_USERNAME"),
                environment.getProperty("DATABASE_USERNAME"),
                environment.getProperty("DB_USERNAME")));
    }

    private static boolean hasExplicitPassword(ConfigurableEnvironment environment) {
        return StringUtils.hasText(firstText(
                environment.getProperty("SPRING_DATASOURCE_PASSWORD"),
                environment.getProperty("DATABASE_PASSWORD"),
                environment.getProperty("DB_PASSWORD")));
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private record NormalizedDatabaseUrl(String jdbcUrl, String username, String password) {
    }
}
