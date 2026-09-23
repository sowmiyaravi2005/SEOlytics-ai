package com.seolytics.crawler;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RobotsTxtRulesTest {

    @Test
    void respectsDisallowAndAllow() {
        String body = """
                User-agent: *
                Disallow: /private
                Allow: /private/open
                Crawl-delay: 1
                """;
        RobotsTxtRules rules = RobotsTxtRules.parse(body, 400);
        assertFalse(rules.isAllowed(URI.create("https://example.com/private/secret")));
        assertTrue(rules.isAllowed(URI.create("https://example.com/private/open")));
        assertTrue(rules.isAllowed(URI.create("https://example.com/about")));
        assertTrue(rules.getCrawlDelayMs() >= 1000);
    }
}
