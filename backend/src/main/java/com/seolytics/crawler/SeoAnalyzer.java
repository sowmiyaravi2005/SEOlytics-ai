package com.seolytics.crawler;

import com.seolytics.domain.IssueSeverity;
import com.seolytics.domain.IssueType;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SeoAnalyzer {

    private static final Pattern GA4_MEASUREMENT_ID = Pattern.compile("\\bG-[A-Z0-9]{6,}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern GTM_ID = Pattern.compile("\\bGTM-[A-Z0-9]+\\b", Pattern.CASE_INSENSITIVE);

    private SeoAnalyzer() {
    }

    public static PageSignals extract(Document document, String pageUrl, FetchResult fetch) {
        PageSignals signals = new PageSignals();
        signals.url = pageUrl;
        signals.statusCode = fetch.getStatusCode();
        signals.redirectHops = fetch.getRedirectHops();
        signals.fetchTimeMs = fetch.getElapsedMs();
        signals.https = pageUrl.toLowerCase(Locale.ROOT).startsWith("https://");
        if (document == null) {
            return signals;
        }
        signals.title = textOrNull(document.title());
        Element desc = document.selectFirst("meta[name=description]");
        signals.metaDescription = desc == null ? null : textOrNull(desc.attr("content"));
        Element canonical = document.selectFirst("link[rel=canonical]");
        signals.canonical = canonical == null ? null : textOrNull(canonical.attr("abs:href"));
        if (signals.canonical == null && canonical != null) {
            signals.canonical = textOrNull(canonical.attr("href"));
        }
        Elements h1s = document.select("h1");
        signals.h1Count = h1s.size();
        signals.h1Text = h1s.eachText().stream().limit(5).reduce((a, b) -> a + " | " + b).orElse(null);
        Elements images = document.select("img");
        signals.imageCount = images.size();
        signals.imagesMissingAlt = (int) images.stream()
                .filter(img -> img.attr("alt") == null || img.attr("alt").isBlank())
                .count();
        signals.hasViewport = document.selectFirst("meta[name=viewport]") != null;
        String html = document.html();
        signals.ga4TagDetected = detectGa4(html);
        return signals;
    }

    public static boolean detectGa4(String html) {
        if (html == null || html.isBlank()) {
            return false;
        }
        Matcher ga = GA4_MEASUREMENT_ID.matcher(html);
        Matcher gtm = GTM_ID.matcher(html);
        boolean gtag = html.toLowerCase(Locale.ROOT).contains("gtag(")
                || html.toLowerCase(Locale.ROOT).contains("googletagmanager.com/gtag/js")
                || html.toLowerCase(Locale.ROOT).contains("googletagmanager.com/gtm.js");
        return ga.find() || gtm.find() || gtag;
    }

    public static List<IssueDraft> pageIssues(PageSignals signals) {
        List<IssueDraft> issues = new ArrayList<>();
        if (signals.statusCode >= 500) {
            issues.add(issue(IssueType.HTTP_SERVER_ERROR, IssueSeverity.CRITICAL,
                    "Server error response",
                    "The page returned HTTP " + signals.statusCode + ".",
                    "Investigate server logs and restore a successful 200 response for indexable URLs."));
        } else if (signals.statusCode >= 400) {
            issues.add(issue(IssueType.HTTP_CLIENT_ERROR, IssueSeverity.HIGH,
                    "Client error response",
                    "The page returned HTTP " + signals.statusCode + ".",
                    "Fix or redirect the URL so crawlers receive 200 or a single, intentional redirect."));
        }
        if (signals.redirectHops >= 3) {
            issues.add(issue(IssueType.REDIRECT_CHAIN, IssueSeverity.HIGH,
                    "Redirect chain detected",
                    "This URL required " + signals.redirectHops + " redirect hops before a final response.",
                    "Point the original URL directly to the final destination to avoid redirect chains."));
        }
        if (!signals.https) {
            issues.add(issue(IssueType.INSECURE_HTTP, IssueSeverity.HIGH,
                    "Page is not served over HTTPS",
                    "The crawled URL uses HTTP rather than HTTPS.",
                    "Enable TLS and redirect HTTP to HTTPS across the site."));
        }
        boolean htmlOk = signals.statusCode > 0 && signals.statusCode < 400;
        if (htmlOk) {
            if (signals.title == null) {
                issues.add(issue(IssueType.MISSING_TITLE, IssueSeverity.CRITICAL,
                        "Missing title tag",
                        "No <title> element was found on this page.",
                        "Add a unique, descriptive title of about 50–60 characters."));
            } else if (signals.title.isBlank()) {
                issues.add(issue(IssueType.EMPTY_TITLE, IssueSeverity.CRITICAL,
                        "Empty title tag",
                        "A title tag exists but contains no text.",
                        "Populate the title with a unique description of the page topic."));
            }
            if (signals.metaDescription == null || signals.metaDescription.isBlank()) {
                issues.add(issue(IssueType.MISSING_META_DESCRIPTION, IssueSeverity.MEDIUM,
                        "Missing meta description",
                        "No usable meta description was found.",
                        "Write a unique 140–160 character summary that matches search intent."));
            }
            if (signals.canonical == null || signals.canonical.isBlank()) {
                issues.add(issue(IssueType.MISSING_CANONICAL, IssueSeverity.MEDIUM,
                        "Missing canonical tag",
                        "No rel=canonical link was found.",
                        "Add a self-referencing canonical URL to reduce duplicate indexing risk."));
            }
            if (signals.h1Count == 0) {
                issues.add(issue(IssueType.MISSING_H1, IssueSeverity.HIGH,
                        "Missing H1 heading",
                        "The page does not contain an H1 heading.",
                        "Add one clear H1 that describes the primary topic of the page."));
            } else if (signals.h1Count > 1) {
                issues.add(issue(IssueType.MULTIPLE_H1, IssueSeverity.LOW,
                        "Multiple H1 headings",
                        "Found " + signals.h1Count + " H1 headings.",
                        "Keep a single primary H1 and use H2–H4 for supporting sections."));
            }
            if (!Boolean.TRUE.equals(signals.hasViewport)) {
                issues.add(issue(IssueType.MISSING_VIEWPORT, IssueSeverity.MEDIUM,
                        "Missing viewport meta tag",
                        "No viewport meta tag was detected.",
                        "Add <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"> for mobile usability."));
            }
            if (signals.imagesMissingAlt != null && signals.imagesMissingAlt > 0) {
                issues.add(issue(IssueType.MISSING_IMAGE_ALT, IssueSeverity.LOW,
                        "Images missing alt text",
                        signals.imagesMissingAlt + " image(s) are missing an alt attribute.",
                        "Provide concise alt text for informative images; use empty alt for decorative ones."));
            }
        }
        return issues;
    }

    private static IssueDraft issue(IssueType type, IssueSeverity severity, String title, String description, String recommendation) {
        return new IssueDraft(type, severity, title, description, recommendation);
    }

    private static String textOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static class PageSignals {
        public String url;
        public int statusCode;
        public int redirectHops;
        public long fetchTimeMs;
        public String title;
        public String metaDescription;
        public String canonical;
        public Integer h1Count;
        public String h1Text;
        public Integer imageCount;
        public Integer imagesMissingAlt;
        public Boolean hasViewport;
        public Boolean https;
        public Boolean ga4TagDetected;
    }

    public record IssueDraft(IssueType type, IssueSeverity severity, String title, String description, String recommendation) {
    }
}
