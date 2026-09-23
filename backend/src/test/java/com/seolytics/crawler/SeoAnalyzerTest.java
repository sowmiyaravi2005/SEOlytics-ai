package com.seolytics.crawler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeoAnalyzerTest {

    @Test
    void detectsCommonGa4Snippets() {
        assertTrue(SeoAnalyzer.detectGa4("gtag('config', 'G-ABC123DEF4');"));
        assertTrue(SeoAnalyzer.detectGa4("https://www.googletagmanager.com/gtm.js?id=GTM-XXXX"));
        assertFalse(SeoAnalyzer.detectGa4("<html><title>No analytics</title></html>"));
    }
}
