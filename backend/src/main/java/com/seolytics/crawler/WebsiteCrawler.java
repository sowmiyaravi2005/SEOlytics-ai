package com.seolytics.crawler;

import com.seolytics.config.SeolyticsProperties;
import com.seolytics.domain.CrawlStatus;
import com.seolytics.domain.IssueSeverity;
import com.seolytics.domain.IssueType;
import com.seolytics.entity.CrawledPage;
import com.seolytics.entity.CrawlSession;
import com.seolytics.entity.SeoIssue;
import com.seolytics.repository.CrawledPageRepository;
import com.seolytics.repository.CrawlSessionRepository;
import com.seolytics.repository.SeoIssueRepository;
import com.seolytics.service.SeoFixGuidanceLibrary;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class WebsiteCrawler {

    private static final Logger log = LoggerFactory.getLogger(WebsiteCrawler.class);

    private final CrawlSessionRepository crawlSessionRepository;
    private final CrawledPageRepository crawledPageRepository;
    private final SeoIssueRepository seoIssueRepository;
    private final HttpFetcher httpFetcher;
    private final SeleniumRenderer seleniumRenderer;
    private final SeoScoreCalculator seoScoreCalculator;
    private final UrlSafetyValidator urlSafetyValidator;
    private final SeolyticsProperties properties;
    private final SeoFixGuidanceLibrary fixGuidanceLibrary;

    public WebsiteCrawler(CrawlSessionRepository crawlSessionRepository,
                          CrawledPageRepository crawledPageRepository,
                          SeoIssueRepository seoIssueRepository,
                          HttpFetcher httpFetcher,
                          SeleniumRenderer seleniumRenderer,
                          SeoScoreCalculator seoScoreCalculator,
                          UrlSafetyValidator urlSafetyValidator,
                          SeolyticsProperties properties,
                          SeoFixGuidanceLibrary fixGuidanceLibrary) {
        this.crawlSessionRepository = crawlSessionRepository;
        this.crawledPageRepository = crawledPageRepository;
        this.seoIssueRepository = seoIssueRepository;
        this.httpFetcher = httpFetcher;
        this.seleniumRenderer = seleniumRenderer;
        this.seoScoreCalculator = seoScoreCalculator;
        this.urlSafetyValidator = urlSafetyValidator;
        this.properties = properties;
        this.fixGuidanceLibrary = fixGuidanceLibrary;
    }

    @Async("crawlExecutor")
    public void crawlAsync(Long sessionId) {
        crawl(sessionId);
    }

    public void crawl(Long sessionId) {
        CrawlSession session = crawlSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return;
        }
        session.setStatus(CrawlStatus.RUNNING);
        session.setStartedAt(Instant.now());
        session.setProgressPercent(2);
        session.setProgressMessage("Validating start URL and fetching robots.txt");
        crawlSessionRepository.save(session);

        List<SeoIssue> collectedIssues = new ArrayList<>();
        List<CrawledPage> crawledPages = new ArrayList<>();
        try {
            URI start = urlSafetyValidator.validatePublicHttpUrl(session.getStartUrl());
            String origin = originOf(start);
            RobotsTxtRules robots = loadRobots(origin, session, collectedIssues);
            Set<String> seeds = loadSitemap(origin, start, session, collectedIssues, robots);
            seeds.add(normalizeUrl(start.toString()));

            Deque<CrawlTarget> queue = new ArrayDeque<>();
            Set<String> seen = new HashSet<>();
            for (String seed : seeds) {
                enqueue(queue, seen, seed, 0, session.getMaxDepth(), origin, robots);
            }
            if (queue.isEmpty()) {
                enqueue(queue, seen, normalizeUrl(start.toString()), 0, session.getMaxDepth(), origin, robots);
            }

            boolean ga4Any = false;
            int processed = 0;
            while (!queue.isEmpty() && processed < session.getMaxPages()) {
                CrawlTarget target = queue.poll();
                session.setProgressMessage("Crawling " + target.url);
                session.setProgressPercent(Math.min(90, 8 + (int) ((processed / (double) session.getMaxPages()) * 80)));
                crawlSessionRepository.save(session);

                politeDelay(robots.getCrawlDelayMs());
                FetchResult fetch = httpFetcher.fetch(target.url, true);
                Document document = parseHtml(fetch);
                if (processed == 0 && shouldUseSelenium(document, fetch)) {
                    String rendered = seleniumRenderer.render(target.url);
                    if (rendered != null && !rendered.isBlank()) {
                        document = Jsoup.parse(rendered, target.url);
                    }
                }

                SeoAnalyzer.PageSignals signals = SeoAnalyzer.extract(document, fetch.getFinalUrl() == null ? target.url : fetch.getFinalUrl(), fetch);
                CrawledPage page = persistPage(session, signals, target.depth);
                crawledPages.add(page);
                ga4Any = ga4Any || Boolean.TRUE.equals(signals.ga4TagDetected);

                for (SeoAnalyzer.IssueDraft draft : SeoAnalyzer.pageIssues(signals)) {
                    collectedIssues.add(toIssue(session, page, page.getUrl(), draft));
                }
                checkBrokenLinks(document, origin, session, page, collectedIssues);

                if (document != null && target.depth < session.getMaxDepth()) {
                    for (String href : extractInternalLinks(document, origin)) {
                        enqueue(queue, seen, href, target.depth + 1, session.getMaxDepth(), origin, robots);
                    }
                }
                processed++;
                session.setPagesCrawled(processed);
                crawlSessionRepository.save(session);
            }

            addDuplicateIssues(session, crawledPages, collectedIssues);
            if (!Boolean.TRUE.equals(ga4Any)) {
                collectedIssues.add(toIssue(session, crawledPages.isEmpty() ? null : crawledPages.get(0),
                        session.getStartUrl(),
                        new SeoAnalyzer.IssueDraft(IssueType.GA4_TAG_NOT_DETECTED, IssueSeverity.LOW,
                                "GA4 / Google tag not detected",
                                "No common GA4 measurement ID (G-...), gtag snippet, or Google Tag Manager container was found in crawled HTML. Tag presence is not proof that analytics data collection is working.",
                                "Install GA4 via gtag.js or Google Tag Manager, then verify hits in GA4 DebugView. SEOlytics only detects snippets in HTML, not live data collection.")));
            }

            seoIssueRepository.saveAll(collectedIssues);
            updatePreviousIssueVerification(session, collectedIssues);
            SeoScoreCalculator.ScoreResult score = seoScoreCalculator.calculate(collectedIssues);
            int healthy = (int) crawledPages.stream()
                    .filter(p -> collectedIssues.stream().noneMatch(i -> i.getPage() != null && i.getPage().getId().equals(p.getId())))
                    .count();

            session.setGa4TagDetected(ga4Any);
            session.setIssuesFound(collectedIssues.size());
            session.setHealthyPages(healthy);
            session.setSeoScore(score.score());
            session.setScoringBreakdown(score.breakdown());
            session.setFinishedAt(Instant.now());
            session.setProgressPercent(100);
            if (crawledPages.isEmpty()) {
                session.setStatus(CrawlStatus.FAILED);
                session.setErrorMessage("No pages could be crawled. The site may be blocking requests or is unreachable.");
                session.setProgressMessage("Crawl failed");
            } else if (session.getErrorMessage() != null && !session.getErrorMessage().isBlank()) {
                session.setStatus(CrawlStatus.PARTIAL);
                session.setProgressMessage("Crawl finished with partial results");
            } else {
                session.setStatus(CrawlStatus.COMPLETED);
                session.setProgressMessage("Crawl complete");
            }
            crawlSessionRepository.save(session);
        } catch (Exception ex) {
            log.error("Crawl failed for session {}", sessionId, ex);
            session.setStatus(crawledPages.isEmpty() ? CrawlStatus.FAILED : CrawlStatus.PARTIAL);
            session.setErrorMessage(ex.getMessage());
            session.setFinishedAt(Instant.now());
            session.setProgressMessage("Crawl failed");
            session.setProgressPercent(100);
            session.setIssuesFound(collectedIssues.size());
            if (!collectedIssues.isEmpty()) {
                seoIssueRepository.saveAll(collectedIssues);
                SeoScoreCalculator.ScoreResult score = seoScoreCalculator.calculate(collectedIssues);
                session.setSeoScore(score.score());
                session.setScoringBreakdown(score.breakdown());
            }
            crawlSessionRepository.save(session);
        }
    }

    private CrawledPage persistPage(CrawlSession session, SeoAnalyzer.PageSignals signals, int depth) {
        CrawledPage page = new CrawledPage();
        page.setSession(session);
        page.setUrl(signals.url);
        page.setStatusCode(signals.statusCode);
        page.setRedirectHops(signals.redirectHops);
        page.setDepth(depth);
        page.setFetchTimeMs(signals.fetchTimeMs);
        page.setTitle(truncate(signals.title, 512));
        page.setMetaDescription(truncate(signals.metaDescription, 1024));
        page.setCanonical(truncate(signals.canonical, 2048));
        page.setH1Count(signals.h1Count);
        page.setH1Text(signals.h1Text);
        page.setImageCount(signals.imageCount);
        page.setImagesMissingAlt(signals.imagesMissingAlt);
        page.setHasViewport(signals.hasViewport);
        page.setHttps(signals.https);
        page.setGa4TagDetected(signals.ga4TagDetected);
        page.setIndexable(signals.statusCode >= 200 && signals.statusCode < 400);
        return crawledPageRepository.save(page);
    }

    private void checkBrokenLinks(Document document, String origin, CrawlSession session, CrawledPage page, List<SeoIssue> issues) {
        if (document == null) {
            return;
        }
        int checked = 0;
        Set<String> seen = new HashSet<>();
        for (Element link : document.select("a[href]")) {
            if (checked >= properties.getCrawler().getMaxLinkChecksPerPage()) {
                break;
            }
            String abs = link.absUrl("href");
            if (abs == null || abs.isBlank() || abs.startsWith("mailto:") || abs.startsWith("tel:") || abs.startsWith("javascript:")) {
                continue;
            }
            String normalized = normalizeUrl(abs);
            if (!seen.add(normalized) || !urlSafetyValidator.isSafeHttpUrl(normalized)) {
                continue;
            }
            FetchResult status = httpFetcher.headOrGetStatus(normalized);
            checked++;
            if (status.getStatusCode() >= 400 || status.getStatusCode() == 0) {
                boolean internal = normalized.startsWith(origin);
                String anchor = link.text() == null || link.text().isBlank() ? "(no anchor text)" : link.text().strip();
                String statusText = status.getStatusCode() == 0 ? status.getError() : String.valueOf(status.getStatusCode());
                issues.add(toIssue(session, page, page.getUrl(),
                        new SeoAnalyzer.IssueDraft(
                                internal ? IssueType.BROKEN_INTERNAL_LINK : IssueType.BROKEN_EXTERNAL_LINK,
                                internal ? IssueSeverity.HIGH : IssueSeverity.MEDIUM,
                                internal ? "Broken internal link" : "Broken external link",
                                "Source page: " + page.getUrl() + "\nBroken URL: " + normalized + "\nHTTP status: " + statusText + "\nAnchor text: " + anchor,
                                "Update or remove the link, or restore the destination URL.")));
            }
        }
    }

    private void addDuplicateIssues(CrawlSession session, List<CrawledPage> pages, List<SeoIssue> issues) {
        Map<String, List<CrawledPage>> byTitle = new HashMap<>();
        Map<String, List<CrawledPage>> byDesc = new HashMap<>();
        for (CrawledPage page : pages) {
            if (page.getTitle() != null && !page.getTitle().isBlank()) {
                byTitle.computeIfAbsent(page.getTitle().toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(page);
            }
            if (page.getMetaDescription() != null && !page.getMetaDescription().isBlank()) {
                byDesc.computeIfAbsent(page.getMetaDescription().toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(page);
            }
        }
        byTitle.values().stream().filter(list -> list.size() > 1).forEach(list -> {
            for (CrawledPage page : list) {
                issues.add(toIssue(session, page, page.getUrl(), new SeoAnalyzer.IssueDraft(
                        IssueType.DUPLICATE_TITLE, IssueSeverity.HIGH, "Duplicate page title",
                        "The title \"" + page.getTitle() + "\" is reused on " + list.size() + " crawled pages.",
                        "Give each indexable page a unique title that reflects its distinct topic.")));
            }
        });
        byDesc.values().stream().filter(list -> list.size() > 1).forEach(list -> {
            for (CrawledPage page : list) {
                issues.add(toIssue(session, page, page.getUrl(), new SeoAnalyzer.IssueDraft(
                        IssueType.DUPLICATE_META_DESCRIPTION, IssueSeverity.MEDIUM, "Duplicate meta description",
                        "The same meta description is reused on " + list.size() + " crawled pages.",
                        "Write a unique meta description for each important page.")));
            }
        });
    }

    private RobotsTxtRules loadRobots(String origin, CrawlSession session, List<SeoIssue> issues) {
        FetchResult robotsFetch = httpFetcher.fetch(origin + "/robots.txt", true);
        boolean accessible = robotsFetch.getStatusCode() >= 200 && robotsFetch.getStatusCode() < 400 && robotsFetch.getBody() != null;
        session.setRobotsTxtAccessible(accessible);
        if (!accessible) {
            issues.add(toIssue(session, null, origin + "/robots.txt", new SeoAnalyzer.IssueDraft(
                    IssueType.ROBOTS_TXT_INACCESSIBLE, IssueSeverity.MEDIUM, "robots.txt is not accessible",
                    "Could not read robots.txt (HTTP " + robotsFetch.getStatusCode() + "). Crawl-delay and disallow rules could not be applied from a live file.",
                    "Publish a valid robots.txt at the site root so crawlers can discover sitemaps and restrictions.")));
            return RobotsTxtRules.parse("", properties.getCrawler().getDelayMs());
        }
        return RobotsTxtRules.parse(robotsFetch.getBody(), properties.getCrawler().getDelayMs());
    }

    private Set<String> loadSitemap(String origin, URI start, CrawlSession session, List<SeoIssue> issues, RobotsTxtRules robots) {
        Set<String> urls = new HashSet<>();
        List<String> candidates = List.of(origin + "/sitemap.xml", origin + "/sitemap_index.xml");
        boolean found = false;
        boolean valid = false;
        for (String candidate : candidates) {
            FetchResult fetch = httpFetcher.fetch(candidate, true);
            if (fetch.getStatusCode() >= 200 && fetch.getStatusCode() < 400 && fetch.getBody() != null && fetch.getBody().contains("<")) {
                found = true;
                try {
                    Document xml = Jsoup.parse(fetch.getBody(), candidate, org.jsoup.parser.Parser.xmlParser());
                    for (Element loc : xml.select("loc")) {
                        String locUrl = loc.text();
                        if (locUrl != null && locUrl.toLowerCase(Locale.ROOT).contains("sitemap") && locUrl.toLowerCase(Locale.ROOT).endsWith(".xml")) {
                            FetchResult nested = httpFetcher.fetch(locUrl, true);
                            if (nested.getStatusCode() >= 200 && nested.getBody() != null) {
                                Document nestedXml = Jsoup.parse(nested.getBody(), locUrl, org.jsoup.parser.Parser.xmlParser());
                                extractLocs(nestedXml, origin, robots, urls, session.getMaxPages());
                            }
                        } else {
                            addIfInternal(locUrl, origin, robots, urls, session.getMaxPages());
                        }
                    }
                    valid = !urls.isEmpty() || xml.select("url, sitemap").size() > 0;
                } catch (Exception ex) {
                    valid = false;
                }
            }
            if (found) {
                break;
            }
        }
        session.setSitemapFound(found);
        session.setSitemapValid(valid);
        if (!found) {
            issues.add(toIssue(session, null, origin + "/sitemap.xml", new SeoAnalyzer.IssueDraft(
                    IssueType.MISSING_SITEMAP, IssueSeverity.MEDIUM, "sitemap.xml not found",
                    "No sitemap.xml or sitemap index was found at the common root locations. Internal links will be used for discovery.",
                    "Publish an XML sitemap and reference it in robots.txt.")));
        } else if (!valid) {
            issues.add(toIssue(session, null, origin + "/sitemap.xml", new SeoAnalyzer.IssueDraft(
                    IssueType.INVALID_SITEMAP, IssueSeverity.MEDIUM, "sitemap.xml could not be parsed",
                    "A sitemap response was found but no usable <loc> entries were extracted.",
                    "Validate the sitemap XML and ensure it lists canonical page URLs.")));
        }
        return urls;
    }

    private void extractLocs(Document xml, String origin, RobotsTxtRules robots, Set<String> urls, int cap) {
        for (Element loc : xml.select("loc")) {
            addIfInternal(loc.text(), origin, robots, urls, cap);
        }
    }

    private void addIfInternal(String locUrl, String origin, RobotsTxtRules robots, Set<String> urls, int cap) {
        if (urls.size() >= cap || locUrl == null || locUrl.isBlank()) {
            return;
        }
        String normalized = normalizeUrl(locUrl.strip());
        if (normalized.startsWith(origin) && urlSafetyValidator.isSafeHttpUrl(normalized)) {
            try {
                if (robots.isAllowed(URI.create(normalized))) {
                    urls.add(normalized);
                }
            } catch (Exception ignored) {
                // skip malformed loc
            }
        }
    }

    private List<String> extractInternalLinks(Document document, String origin) {
        List<String> links = new ArrayList<>();
        for (Element a : document.select("a[href]")) {
            String abs = a.absUrl("href");
            if (abs != null && abs.startsWith(origin)) {
                String normalized = normalizeUrl(abs);
                if (urlSafetyValidator.isSafeHttpUrl(normalized)) {
                    links.add(normalized);
                }
            }
        }
        return links;
    }

    private void enqueue(Deque<CrawlTarget> queue, Set<String> seen, String url, int depth, int maxDepth, String origin, RobotsTxtRules robots) {
        if (depth > maxDepth || url == null || !url.startsWith(origin)) {
            return;
        }
        String normalized = normalizeUrl(url);
        if (!seen.add(normalized) || !urlSafetyValidator.isSafeHttpUrl(normalized)) {
            return;
        }
        try {
            URI uri = URI.create(normalized);
            String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
            if (path.matches(".*\\.(pdf|jpg|jpeg|png|gif|svg|webp|zip|css|js|mp4|mp3)$")) {
                return;
            }
            if (!robots.isAllowed(uri)) {
                return;
            }
            queue.add(new CrawlTarget(normalized, depth));
        } catch (Exception ignored) {
            seen.remove(normalized);
        }
    }

    private Document parseHtml(FetchResult fetch) {
        if (fetch.getBody() == null || fetch.getBody().isBlank() || fetch.getStatusCode() == 0) {
            return null;
        }
        try {
            return Jsoup.parse(fetch.getBody(), fetch.getFinalUrl());
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean shouldUseSelenium(Document document, FetchResult fetch) {
        if (!properties.getCrawler().isUseSelenium()) {
            return false;
        }
        if (document == null) {
            return fetch.getStatusCode() >= 200 && fetch.getStatusCode() < 400;
        }
        String text = document.text();
        return text == null || text.length() < 80;
    }

    private SeoIssue toIssue(CrawlSession session, CrawledPage page, String pageUrl, SeoAnalyzer.IssueDraft draft) {
        SeoIssue issue = new SeoIssue();
        issue.setSession(session);
        issue.setPage(page);
        issue.setPageUrl(pageUrl);
        issue.setIssueType(draft.type());
        issue.setSeverity(draft.severity());
        issue.setTitle(draft.title());
        issue.setDescription(draft.description());
        issue.setRecommendation(draft.recommendation());
        SeoFixGuidanceLibrary.FixGuidance guidance = fixGuidanceLibrary.forType(draft.type());
        issue.setDetectedValue(draft.description());
        issue.setExpectedValue(guidance.expectedValue());
        issue.setExplanation(guidance.problem());
        issue.setWhyItMatters(guidance.whyItMatters());
        issue.setHowToFix(guidance.howToFix());
        issue.setExampleSolution(guidance.exampleSolution());
        issue.setVerificationMethod(guidance.verificationMethod());
        return issue;
    }

    private void updatePreviousIssueVerification(CrawlSession current, List<SeoIssue> currentIssues) {
        crawlSessionRepository.findTopByWebsiteAndIdLessThanOrderByIdDesc(current.getWebsite(), current.getId())
                .ifPresent(previous -> {
                    Set<String> currentSignatures = new HashSet<>();
                    currentIssues.forEach(issue -> currentSignatures.add(issueSignature(issue)));
                    List<SeoIssue> previousIssues = seoIssueRepository.findBySessionOrderBySeverityAscIdAsc(previous);
                    boolean changed = false;
                    for (SeoIssue previousIssue : previousIssues) {
                        if (previousIssue.getStatus() != null && previousIssue.getStatus().name().equals("IGNORED")) {
                            continue;
                        }
                        if (!currentSignatures.contains(issueSignature(previousIssue))) {
                            previousIssue.setStatus(com.seolytics.domain.IssueStatus.FIXED);
                            changed = true;
                        }
                    }
                    if (changed) {
                        seoIssueRepository.saveAll(previousIssues);
                    }
                });
    }

    private String issueSignature(SeoIssue issue) {
        return issue.getIssueType().name() + "|" + (issue.getPageUrl() == null ? "" : issue.getPageUrl().toLowerCase(Locale.ROOT));
    }

    private String originOf(URI uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();
        if (port > 0 && port != 80 && port != 443) {
            return scheme + "://" + host + ":" + port;
        }
        return scheme + "://" + host;
    }

    private String normalizeUrl(String url) {
        try {
            URI uri = URI.create(url).normalize();
            String path = uri.getPath() == null || uri.getPath().isBlank() ? "/" : uri.getPath();
            if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            StringBuilder builder = new StringBuilder();
            builder.append(uri.getScheme()).append("://").append(uri.getHost().toLowerCase(Locale.ROOT));
            if (uri.getPort() > 0 && uri.getPort() != 80 && uri.getPort() != 443) {
                builder.append(":").append(uri.getPort());
            }
            builder.append(path);
            return builder.toString();
        } catch (Exception ex) {
            return url;
        }
    }

    private void politeDelay(int delayMs) {
        try {
            Thread.sleep(Math.max(200, delayMs));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private record CrawlTarget(String url, int depth) {
    }
}
