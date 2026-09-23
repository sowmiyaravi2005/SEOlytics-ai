package com.seolytics.service;

import com.seolytics.domain.IssueType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class SeoFixGuidanceLibrary {

    private final Map<IssueType, FixGuidance> guidance = new EnumMap<>(IssueType.class);

    public SeoFixGuidanceLibrary() {
        add(IssueType.MISSING_TITLE,
                "Page has no <title> element.",
                "The title is one of the clearest signals search engines and users see when deciding what a page is about.",
                "Add one descriptive and unique <title> inside the <head> section. Keep it aligned with the page's actual content.",
                "<title>Descriptive Page Title | Website Name</title>",
                "A unique, non-empty title element.",
                "Re-crawl the page and confirm that the crawler detects a populated title.");
        add(IssueType.EMPTY_TITLE,
                "The page has a <title> element, but it is empty.",
                "Empty titles make search snippets weaker and make it harder to distinguish pages in browser tabs, bookmarks, and search results.",
                "Replace the empty title content with a concise title that names the page topic and, where useful, the brand.",
                "<title>Technical SEO Audit Services | Example Agency</title>",
                "A title element with meaningful text.",
                "Re-crawl and confirm the title field is no longer blank.");
        add(IssueType.DUPLICATE_TITLE,
                "Multiple crawled pages use the same title.",
                "Duplicate titles make pages harder to differentiate and can cause search engines to choose less helpful snippets.",
                "Write a distinct title for each affected URL based on that page's actual subject, location, product, or intent.",
                "<title>Enterprise SEO Audit Pricing | SEOlytics</title>",
                "Unique title text for each important page.",
                "Re-crawl and confirm this duplicate title group no longer appears.");
        add(IssueType.MISSING_META_DESCRIPTION,
                "Meta description is missing from the page.",
                "A useful meta description can give search engines and users a concise summary of the page.",
                "Add a unique and relevant meta description inside the <head> section.",
                "<meta name=\"description\" content=\"Explore our web development services and digital solutions.\">",
                "A unique, relevant meta description.",
                "Re-crawl the page and confirm that the meta description is detected.");
        add(IssueType.DUPLICATE_META_DESCRIPTION,
                "Multiple pages reuse the same meta description.",
                "Repeated descriptions make different pages look interchangeable in search results and can reduce click clarity.",
                "Create a unique description for each affected page that summarizes that page's specific content and user value.",
                "<meta name=\"description\" content=\"Compare technical SEO audit plans for ecommerce websites.\">",
                "Unique meta descriptions for each important page.",
                "Re-crawl and confirm duplicate meta description issues are reduced or gone.");
        add(IssueType.MISSING_H1,
                "The page does not contain an H1 heading.",
                "The H1 helps users and crawlers understand the primary topic of the page.",
                "Add one visible H1 that clearly represents the page's main topic.",
                "<h1>Main Topic of the Page</h1>",
                "Exactly one clear primary H1 on the page.",
                "Re-crawl and confirm the H1 count is 1 and the H1 text matches the page topic.");
        add(IssueType.MULTIPLE_H1,
                "The page contains more than one H1 heading.",
                "Multiple primary headings can blur the page hierarchy and make content structure less clear.",
                "Choose the primary heading for the page, keep it as the H1, and convert supporting headings to H2 or H3 as appropriate.",
                "<h1>Technical SEO Audit</h1>\n<h2>Crawl Coverage</h2>\n<h2>Indexing Checks</h2>",
                "One primary H1, with supporting sections using lower heading levels.",
                "Re-crawl and confirm the H1 count has been reduced to 1.");
        add(IssueType.MISSING_IMAGE_ALT,
                "One or more images are missing alt text.",
                "Alt text improves accessibility and helps search engines understand informative images.",
                "Add accurate alt text for informative images. Use an empty alt attribute for purely decorative images and avoid keyword stuffing.",
                "<img src=\"product.jpg\" alt=\"Blue running shoes\">",
                "Informative images have descriptive alt text; decorative images have alt=\"\".",
                "Re-crawl and confirm the missing-alt count has decreased.");
        add(IssueType.BROKEN_INTERNAL_LINK,
                "An internal link points to a URL that returned an error.",
                "Broken internal links waste crawl paths and create a poor user experience.",
                "Correct the URL, replace it with the right destination, remove it if no longer relevant, or add an appropriate redirect when a page permanently moved.",
                "<a href=\"/services/technical-seo\">Technical SEO services</a>",
                "The linked internal URL returns a successful page or intentional single redirect.",
                "Re-crawl and confirm the broken internal link is no longer detected. The app will not change links automatically.");
        add(IssueType.BROKEN_EXTERNAL_LINK,
                "An external link points to a URL that returned an error.",
                "Broken external references reduce trust and can interrupt users who need supporting information.",
                "Replace the destination, remove the link if it is no longer useful, or link to an updated authoritative source.",
                "<a href=\"https://developers.google.com/search/docs\">Google Search documentation</a>",
                "The external destination is reachable and relevant.",
                "Re-crawl and confirm the external link no longer returns an error.");
        add(IssueType.HTTP_CLIENT_ERROR,
                "The page returned a 4xx HTTP response.",
                "Pages returning 4xx errors usually cannot be indexed or used by visitors.",
                "Restore the page, correct inbound links, or redirect the URL to the most relevant replacement if the old URL has permanently moved.",
                "HTTP/1.1 301 Moved Permanently\nLocation: https://example.com/new-page",
                "A 200 response for live pages, or one intentional redirect for moved pages.",
                "Re-crawl and confirm the affected URL no longer returns a 4xx status.");
        add(IssueType.HTTP_SERVER_ERROR,
                "The page returned a 5xx server response.",
                "Server errors can block crawling and indicate visitors may not be able to access the page.",
                "Check application logs, hosting health, upstream services, and deployment errors, then restore a stable successful response.",
                "HTTP/1.1 200 OK",
                "A stable 2xx response for indexable URLs.",
                "Re-crawl and confirm the affected URL no longer returns a 5xx status.");
        add(IssueType.REDIRECT_CHAIN,
                "The URL requires multiple redirect hops before reaching a final page.",
                "Long redirect chains slow users and crawlers and can weaken crawl efficiency.",
                "Update links and redirect rules so the original URL points directly to the final destination.",
                "Page A -> Page C\n\nAvoid: Page A -> Page B -> Page C",
                "One redirect hop at most for moved URLs.",
                "Re-crawl and confirm redirect hops are reduced.");
        add(IssueType.MISSING_CANONICAL,
                "No canonical URL was detected.",
                "Canonical tags help search engines understand the preferred URL when similar or duplicate versions exist.",
                "Add a self-referencing canonical tag for canonical pages, or point duplicates to the preferred URL. Do not insert canonicals blindly.",
                "<link rel=\"canonical\" href=\"https://example.com/page\">",
                "A valid canonical URL in the <head> section.",
                "Re-crawl and confirm the canonical field is populated with the intended URL.");
        add(IssueType.MISSING_VIEWPORT,
                "The page is missing a viewport meta tag.",
                "Without a viewport tag, pages may render poorly on mobile devices.",
                "Add a responsive viewport meta tag inside the <head> section.",
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">",
                "A viewport meta tag configured for responsive layouts.",
                "Re-crawl and confirm viewport detection is true.");
        add(IssueType.INSECURE_HTTP,
                "The crawled URL uses HTTP instead of HTTPS.",
                "HTTPS protects users and is expected for modern websites.",
                "Configure SSL/TLS, redirect HTTP to HTTPS, update internal links, review canonical URLs, and re-crawl.",
                "RewriteCond %{HTTPS} !=on\nRewriteRule ^ https://%{HTTP_HOST}%{REQUEST_URI} [L,R=301]",
                "HTTPS URLs with consistent redirects, internal links, and canonicals.",
                "Re-crawl using the HTTPS URL and confirm insecure HTTP issues are gone. SEOlytics does not install SSL automatically.");
        add(IssueType.MISSING_SITEMAP,
                "No sitemap.xml or sitemap index was found at common root locations.",
                "A sitemap helps crawlers discover important URLs, especially on larger or less-connected sites.",
                "Create or update sitemap.xml, list canonical URLs, and reference it from robots.txt.",
                "<url>\n    <loc>https://example.com/about</loc>\n</url>",
                "A valid sitemap normally accessible at https://example.com/sitemap.xml.",
                "Re-crawl and confirm sitemap found and sitemap valid are true.");
        add(IssueType.INVALID_SITEMAP,
                "A sitemap was found, but usable URL entries could not be parsed.",
                "Invalid sitemaps can prevent crawlers from discovering the URLs you intended to expose.",
                "Validate the XML, include absolute canonical URLs in <loc> elements, and remove malformed entries.",
                "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n  <url><loc>https://example.com/about</loc></url>\n</urlset>",
                "A parseable XML sitemap with valid <loc> entries.",
                "Re-crawl and confirm sitemap valid is true.");
        add(IssueType.ROBOTS_TXT_INACCESSIBLE,
                "robots.txt could not be read from the site root.",
                "robots.txt communicates crawl restrictions and sitemap locations to search crawlers.",
                "Publish a valid robots.txt at the site root and include a Sitemap directive when available. Do not block important pages accidentally.",
                "User-agent: *\nDisallow:\nSitemap: https://example.com/sitemap.xml",
                "A reachable robots.txt file with intentional rules.",
                "Re-crawl and confirm robots.txt accessible is true. SEOlytics will not modify robots.txt automatically.");
        add(IssueType.GA4_TAG_NOT_DETECTED,
                "No common GA4 Google tag or Google Tag Manager snippet was detected in crawled HTML.",
                "Analytics helps site owners measure traffic and user behavior, but tag detection alone does not prove data collection works.",
                "Add the appropriate Google tag or Google Tag Manager snippet using Google's current official implementation instructions, then verify in GA4 DebugView or Realtime.",
                "<script async src=\"https://www.googletagmanager.com/gtag/js?id=G-XXXXXXXXXX\"></script>\n<script>\n  window.dataLayer = window.dataLayer || [];\n  function gtag(){dataLayer.push(arguments);}\n  gtag('js', new Date());\n  gtag('config', 'G-XXXXXXXXXX');\n</script>",
                "A site-owner-approved GA4 or GTM implementation, verified in Google Analytics.",
                "Re-crawl and confirm a common tag is detected, then verify live data inside GA4. Detection is not proof of collection.");
    }

    public FixGuidance forType(IssueType type) {
        return guidance.getOrDefault(type, new FixGuidance(
                "The crawler detected an SEO issue on this page.",
                "Resolving technical SEO issues helps make pages easier for users and crawlers to understand.",
                "Review the affected URL, fix the underlying implementation, and re-crawl to verify.",
                "",
                "The issue is no longer detected on a fresh crawl.",
                "Re-crawl and confirm this issue type no longer appears for the affected URL."
        ));
    }

    private void add(IssueType type, String problem, String whyItMatters, String howToFix,
                     String exampleSolution, String expectedValue, String verificationMethod) {
        guidance.put(type, new FixGuidance(problem, whyItMatters, howToFix, exampleSolution, expectedValue, verificationMethod));
    }

    public record FixGuidance(
            String problem,
            String whyItMatters,
            String howToFix,
            String exampleSolution,
            String expectedValue,
            String verificationMethod
    ) {
    }
}
