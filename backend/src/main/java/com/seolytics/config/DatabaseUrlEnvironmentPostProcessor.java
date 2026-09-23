package com.seolytics.config;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
        if (StringUtils.hasText(normalized.username())) {
            properties.put("spring.datasource.username", normalized.username());
        }
        if (StringUtils.hasText(normalized.password())) {
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

        String username = null;
        String password = null;
        if (StringUtils.hasText(uri.getRawUserInfo())) {
            String[] userInfo = uri.getRawUserInfo().split(":", 2);
            username = decode(userInfo[0]);
            if (userInfo.length > 1) {
                password = decode(userInfo[1]);
            }
        }

        return new NormalizedDatabaseUrl(jdbcUrl.toString(), username, password);
    }

    private static String jdbcScheme(String scheme) {
        if ("postgres".equalsIgnoreCase(scheme) || "postgresql".equalsIgnoreCase(scheme)) {
            return "postgresql";
        }
        if ("mysql".equalsIgnoreCase(scheme)) {
            return "mysql";
        }
        if ("mariadb".equalsIgnoreCase(scheme)) {
            return "mariadb";
        }
        return null;
    }

    private static String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("UTF-8 is not supported", ex);
        }
    }

    private record NormalizedDatabaseUrl(String jdbcUrl, String username, String password) {
    }
}
