package com.seolytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seolytics")
public class SeolyticsProperties {

    private final Cors cors = new Cors();
    private final Jwt jwt = new Jwt();
    private final Crawler crawler = new Crawler();
    private final Scoring scoring = new Scoring();

    public Cors getCors() {
        return cors;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public Crawler getCrawler() {
        return crawler;
    }

    public Scoring getScoring() {
        return scoring;
    }

    public static class Cors {
        private String allowedOrigins = "https://seolytics-ai.onrender.com,http://localhost:5173,http://127.0.0.1:5173";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Jwt {
        private String secret = "change-this-to-a-long-random-secret-key-at-least-32-chars";
        private long expirationMs = 86400000;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMs() {
            return expirationMs;
        }

        public void setExpirationMs(long expirationMs) {
            this.expirationMs = expirationMs;
        }
    }

    public static class Crawler {
        private int maxPagesDefault = 30;
        private int maxPagesCap = 80;
        private int maxDepthDefault = 3;
        private int maxDepthCap = 5;
        private int requestTimeoutMs = 12000;
        private int delayMs = 450;
        private int maxLinkChecksPerPage = 12;
        private int maxHtmlBytes = 1_500_000;
        private int maxRedirectHops = 5;
        private String userAgent = "SEOlyticsBot/1.0";
        private boolean useSelenium = false;

        public int getMaxPagesDefault() {
            return maxPagesDefault;
        }

        public void setMaxPagesDefault(int maxPagesDefault) {
            this.maxPagesDefault = maxPagesDefault;
        }

        public int getMaxPagesCap() {
            return maxPagesCap;
        }

        public void setMaxPagesCap(int maxPagesCap) {
            this.maxPagesCap = maxPagesCap;
        }

        public int getMaxDepthDefault() {
            return maxDepthDefault;
        }

        public void setMaxDepthDefault(int maxDepthDefault) {
            this.maxDepthDefault = maxDepthDefault;
        }

        public int getMaxDepthCap() {
            return maxDepthCap;
        }

        public void setMaxDepthCap(int maxDepthCap) {
            this.maxDepthCap = maxDepthCap;
        }

        public int getRequestTimeoutMs() {
            return requestTimeoutMs;
        }

        public void setRequestTimeoutMs(int requestTimeoutMs) {
            this.requestTimeoutMs = requestTimeoutMs;
        }

        public int getDelayMs() {
            return delayMs;
        }

        public void setDelayMs(int delayMs) {
            this.delayMs = delayMs;
        }

        public int getMaxLinkChecksPerPage() {
            return maxLinkChecksPerPage;
        }

        public void setMaxLinkChecksPerPage(int maxLinkChecksPerPage) {
            this.maxLinkChecksPerPage = maxLinkChecksPerPage;
        }

        public int getMaxHtmlBytes() {
            return maxHtmlBytes;
        }

        public void setMaxHtmlBytes(int maxHtmlBytes) {
            this.maxHtmlBytes = maxHtmlBytes;
        }

        public int getMaxRedirectHops() {
            return maxRedirectHops;
        }

        public void setMaxRedirectHops(int maxRedirectHops) {
            this.maxRedirectHops = maxRedirectHops;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        public boolean isUseSelenium() {
            return useSelenium;
        }

        public void setUseSelenium(boolean useSelenium) {
            this.useSelenium = useSelenium;
        }
    }

    public static class Scoring {
        private double criticalBase = 8;
        private double highBase = 5;
        private double mediumBase = 2;
        private double lowBase = 1;
        private double extraOccurrence = 0.35;
        private double extraOccurrenceCap = 8;

        public double getCriticalBase() {
            return criticalBase;
        }

        public void setCriticalBase(double criticalBase) {
            this.criticalBase = criticalBase;
        }

        public double getHighBase() {
            return highBase;
        }

        public void setHighBase(double highBase) {
            this.highBase = highBase;
        }

        public double getMediumBase() {
            return mediumBase;
        }

        public void setMediumBase(double mediumBase) {
            this.mediumBase = mediumBase;
        }

        public double getLowBase() {
            return lowBase;
        }

        public void setLowBase(double lowBase) {
            this.lowBase = lowBase;
        }

        public double getExtraOccurrence() {
            return extraOccurrence;
        }

        public void setExtraOccurrence(double extraOccurrence) {
            this.extraOccurrence = extraOccurrence;
        }

        public double getExtraOccurrenceCap() {
            return extraOccurrenceCap;
        }

        public void setExtraOccurrenceCap(double extraOccurrenceCap) {
            this.extraOccurrenceCap = extraOccurrenceCap;
        }
    }
}
