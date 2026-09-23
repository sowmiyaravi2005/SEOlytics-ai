package com.seolytics.crawler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlSafetyValidatorTest {

    private final UrlSafetyValidator validator = new UrlSafetyValidator();

    @Test
    void acceptsPublicHttpsUrl() {
        assertEquals("example.com", validator.validatePublicHttpUrl("https://example.com/path").getHost());
    }

    @Test
    void rejectsLocalhost() {
        assertThrows(RuntimeException.class, () -> validator.validatePublicHttpUrl("http://localhost:8080"));
        assertFalse(validator.isSafeHttpUrl("http://127.0.0.1"));
    }

    @Test
    void rejectsPrivateNetwork() {
        assertFalse(validator.isSafeHttpUrl("http://192.168.1.10"));
        assertFalse(validator.isSafeHttpUrl("http://10.0.0.4/admin"));
    }

    @Test
    void rejectsNonHttp() {
        assertThrows(RuntimeException.class, () -> validator.validatePublicHttpUrl("ftp://example.com"));
    }
}
