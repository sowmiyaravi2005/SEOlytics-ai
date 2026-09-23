package com.seolytics.crawler;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RobotsTxtRules {

    private final List<String> allows = new ArrayList<>();
    private final List<String> disallows = new ArrayList<>();
    private int crawlDelayMs;

    public static RobotsTxtRules parse(String body, int defaultDelayMs) {
        RobotsTxtRules rules = new RobotsTxtRules();
        rules.crawlDelayMs = defaultDelayMs;
        if (body == null || body.isBlank()) {
            return rules;
        }
        boolean relevant = false;
        for (String rawLine : body.split("\\R")) {
            String line = rawLine.strip();
            int comment = line.indexOf('#');
            if (comment >= 0) {
                line = line.substring(0, comment).strip();
            }
            if (line.isEmpty()) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String field = line.substring(0, colon).strip().toLowerCase(Locale.ROOT);
            String value = line.substring(colon + 1).strip();
            if (field.equals("user-agent")) {
                relevant = value.equals("*") || value.toLowerCase(Locale.ROOT).contains("seolytics");
            } else if (relevant && field.equals("disallow")) {
                rules.disallows.add(value);
            } else if (relevant && field.equals("allow")) {
                rules.allows.add(value);
            } else if (relevant && field.equals("crawl-delay")) {
                try {
                    double seconds = Double.parseDouble(value);
                    rules.crawlDelayMs = Math.max(defaultDelayMs, (int) Math.round(seconds * 1000));
                } catch (NumberFormatException ignored) {
                    // keep default delay
                }
            }
        }
        return rules;
    }

    public boolean isAllowed(URI uri) {
        String path = uri.getRawPath() == null || uri.getRawPath().isBlank() ? "/" : uri.getRawPath();
        String bestAllow = longestMatch(allows, path);
        String bestDisallow = longestMatch(disallows, path);
        if (bestDisallow == null) {
            return true;
        }
        if (bestAllow != null && bestAllow.length() >= bestDisallow.length()) {
            return true;
        }
        return bestDisallow.isEmpty();
    }

    private String longestMatch(List<String> prefixes, String path) {
        String best = null;
        for (String prefix : prefixes) {
            if (prefix == null) {
                continue;
            }
            if (prefix.isEmpty() || path.startsWith(prefix)) {
                if (best == null || prefix.length() > best.length()) {
                    best = prefix;
                }
            }
        }
        return best;
    }

    public int getCrawlDelayMs() {
        return crawlDelayMs;
    }
}
