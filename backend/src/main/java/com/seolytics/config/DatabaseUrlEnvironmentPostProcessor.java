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
        String databaseUrl = firstText(environment.getProperty("DATABASE_URL"), environment.getProperty("DB_URL"));
        if (!StringUtils.hasText(databaseUrl) || databaseUrl.startsWith("jdbc:")) {
            return;
        }

        NormalizedDatabaseUrl normalized = normalize(databaseUrl);
        if (normalized == null) {
            return;
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.datasource.url", normalized.jdbcUrl());

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

        return new NormalizedDatabaseUrl(jdbcUrl.toString());
    }

    private static String jdbcScheme(String scheme) {
        if ("mysql".equalsIgnoreCase(scheme)) {
            return "mysql";
        }
        return null;
    }

    private static String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private record NormalizedDatabaseUrl(String jdbcUrl) {
    }
}
