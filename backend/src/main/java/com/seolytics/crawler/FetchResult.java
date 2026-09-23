package com.seolytics.crawler;

import java.util.ArrayList;
import java.util.List;

public class FetchResult {
    private final String finalUrl;
    private final int statusCode;
    private final String body;
    private final String contentType;
    private final int redirectHops;
    private final List<Integer> redirectStatuses;
    private final long elapsedMs;
    private final String error;

    public FetchResult(String finalUrl, int statusCode, String body, String contentType,
                       int redirectHops, List<Integer> redirectStatuses, long elapsedMs, String error) {
        this.finalUrl = finalUrl;
        this.statusCode = statusCode;
        this.body = body;
        this.contentType = contentType;
        this.redirectHops = redirectHops;
        this.redirectStatuses = redirectStatuses == null ? List.of() : List.copyOf(redirectStatuses);
        this.elapsedMs = elapsedMs;
        this.error = error;
    }

    public static FetchResult failure(String url, String error, long elapsedMs) {
        return new FetchResult(url, 0, "", "", 0, new ArrayList<>(), elapsedMs, error);
    }

    public String getFinalUrl() {
        return finalUrl;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    public String getContentType() {
        return contentType;
    }

    public int getRedirectHops() {
        return redirectHops;
    }

    public List<Integer> getRedirectStatuses() {
        return redirectStatuses;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public String getError() {
        return error;
    }

    public boolean isHtml() {
        return contentType == null || contentType.isBlank()
                || contentType.toLowerCase().contains("html")
                || contentType.toLowerCase().contains("xml")
                || contentType.toLowerCase().contains("text");
    }
}
