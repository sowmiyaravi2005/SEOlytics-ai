package com.seolytics.service;

import com.seolytics.config.SeolyticsProperties;
import com.seolytics.crawler.UrlSafetyValidator;
import com.seolytics.crawler.WebsiteCrawler;
import com.seolytics.domain.CrawlStatus;
import com.seolytics.domain.IssueSeverity;
import com.seolytics.domain.IssueStatus;
import com.seolytics.domain.IssueType;
import com.seolytics.dto.AuditDtos;
import com.seolytics.entity.CrawledPage;
import com.seolytics.entity.CrawlSession;
import com.seolytics.entity.SeoIssue;
import com.seolytics.entity.UserAccount;
import com.seolytics.entity.Website;
import com.seolytics.exception.ApiException;
import com.seolytics.repository.CrawledPageRepository;
import com.seolytics.repository.CrawlSessionRepository;
import com.seolytics.repository.SeoIssueRepository;
import com.seolytics.repository.WebsiteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class AuditService {

    private final WebsiteRepository websiteRepository;
    private final CrawlSessionRepository crawlSessionRepository;
    private final CrawledPageRepository crawledPageRepository;
    private final SeoIssueRepository seoIssueRepository;
    private final WebsiteCrawler websiteCrawler;
    private final UrlSafetyValidator urlSafetyValidator;
    private final SeolyticsProperties properties;

    public AuditService(WebsiteRepository websiteRepository,
                        CrawlSessionRepository crawlSessionRepository,
                        CrawledPageRepository crawledPageRepository,
                        SeoIssueRepository seoIssueRepository,
                        WebsiteCrawler websiteCrawler,
                        UrlSafetyValidator urlSafetyValidator,
                        SeolyticsProperties properties) {
        this.websiteRepository = websiteRepository;
        this.crawlSessionRepository = crawlSessionRepository;
        this.crawledPageRepository = crawledPageRepository;
        this.seoIssueRepository = seoIssueRepository;
        this.websiteCrawler = websiteCrawler;
        this.urlSafetyValidator = urlSafetyValidator;
        this.properties = properties;
    }

    public AuditDtos.AuditSummary startAudit(UserAccount user, AuditDtos.StartAuditRequest request) {
        URI uri = urlSafetyValidator.validatePublicHttpUrl(request.getUrl());
        String url = uri.toString();
        String domain = uri.getHost().toLowerCase(Locale.ROOT);
        Website website = websiteRepository.findByOwnerAndDomain(user, domain).orElseGet(() -> {
            Website created = new Website();
            created.setOwner(user);
            created.setDomain(domain);
            created.setUrl(url);
            return websiteRepository.save(created);
        });
        website.setUrl(url);
        websiteRepository.save(website);

        int maxPages = clamp(request.getMaxPages(), properties.getCrawler().getMaxPagesDefault(), 1, properties.getCrawler().getMaxPagesCap());
        int maxDepth = clamp(request.getMaxDepth(), properties.getCrawler().getMaxDepthDefault(), 0, properties.getCrawler().getMaxDepthCap());

        CrawlSession session = new CrawlSession();
        session.setOwner(user);
        session.setWebsite(website);
        session.setStartUrl(url);
        session.setStatus(CrawlStatus.PENDING);
        session.setMaxPages(maxPages);
        session.setMaxDepth(maxDepth);
        session.setProgressPercent(0);
        session.setProgressMessage("Queued");
        crawlSessionRepository.save(session);
        websiteCrawler.crawlAsync(session.getId());
        return toSummary(session);
    }

    public AuditDtos.AuditSummary recrawl(UserAccount user, Long id) {
        CrawlSession previous = requireSession(user, id);
        AuditDtos.StartAuditRequest request = new AuditDtos.StartAuditRequest();
        request.setUrl(previous.getStartUrl());
        request.setMaxPages(previous.getMaxPages());
        request.setMaxDepth(previous.getMaxDepth());
        return startAudit(user, request);
    }

    @Transactional(readOnly = true)
    public List<AuditDtos.AuditSummary> list(UserAccount user) {
        return crawlSessionRepository.findByOwnerOrderByCreatedAtDesc(user).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public AuditDtos.AuditDetail detail(UserAccount user, Long id, String query, IssueSeverity severity, IssueType type, Integer status) {
        CrawlSession session = requireSession(user, id);
        List<AuditDtos.PageDto> pages = crawledPageRepository.findBySessionOrderByIdAsc(session).stream()
                .filter(page -> status == null || Objects.equals(page.getStatusCode(), status))
                .map(this::toPage)
                .toList();
        List<AuditDtos.IssueDto> issues = seoIssueRepository.search(session, severity, type, blankToNull(query)).stream()
                .filter(issue -> status == null || issue.getPage() == null || Objects.equals(issue.getPage().getStatusCode(), status))
                .map(this::toIssue)
                .toList();
        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (IssueSeverity value : IssueSeverity.values()) {
            bySeverity.put(value.name(), seoIssueRepository.countBySessionAndSeverity(session, value));
        }
        return new AuditDtos.AuditDetail(toSummary(session), bySeverity, pages, issues);
    }

    @Transactional(readOnly = true)
    public AuditDtos.ProgressDto progress(UserAccount user, Long id) {
        CrawlSession session = requireSession(user, id);
        return new AuditDtos.ProgressDto(session.getId(), session.getStatus(), session.getProgressPercent(),
                session.getProgressMessage(), session.getPagesCrawled(), session.getMaxPages(), session.getErrorMessage());
    }

    @Transactional(readOnly = true)
    public AuditDtos.PageDto page(UserAccount user, Long pageId) {
        CrawledPage page = crawledPageRepository.findByIdAndSession_Owner_Id(pageId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Page not found"));
        return toPage(page);
    }

    @Transactional(readOnly = true)
    public AuditDtos.DashboardDto dashboard(UserAccount user) {
        long websites = websiteRepository.countByOwner(user);
        long audits = crawlSessionRepository.countByOwner(user);
        long pages = crawlSessionRepository.sumPagesCrawled(user);
        Double avg = crawlSessionRepository.averageScore(user, List.of(CrawlStatus.COMPLETED, CrawlStatus.PARTIAL));
        List<CrawlSession> recentDone = crawlSessionRepository.findTop8ByOwnerAndStatusInOrderByFinishedAtDesc(
                user, List.of(CrawlStatus.COMPLETED, CrawlStatus.PARTIAL, CrawlStatus.FAILED));
        Map<String, Long> severity = new LinkedHashMap<>();
        long healthy = 0;
        long withIssues = 0;
        if (!recentDone.isEmpty()) {
            CrawlSession latest = recentDone.get(0);
            for (IssueSeverity value : IssueSeverity.values()) {
                severity.put(value.name(), seoIssueRepository.countBySessionAndSeverity(latest, value));
            }
            healthy = latest.getHealthyPages();
            withIssues = Math.max(0, latest.getPagesCrawled() - latest.getHealthyPages());
        } else {
            for (IssueSeverity value : IssueSeverity.values()) {
                severity.put(value.name(), 0L);
            }
        }
        List<AuditDtos.IssueCount> top = seoIssueRepository.topIssueTypes(user).stream()
                .limit(6)
                .map(row -> new AuditDtos.IssueCount(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .toList();
        List<AuditDtos.HistoryPoint> history = recentDone.stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(s -> new AuditDtos.HistoryPoint(s.getFinishedAt() == null ? s.getCreatedAt() : s.getFinishedAt(),
                        s.getSeoScore(), s.getPagesCrawled(), s.getIssuesFound()))
                .toList();
        List<AuditDtos.AuditSummary> recent = crawlSessionRepository.findByOwnerOrderByCreatedAtDesc(user).stream()
                .limit(6)
                .map(this::toSummary)
                .toList();
        return new AuditDtos.DashboardDto(websites, audits, pages, avg == null ? null : Math.round(avg * 10.0) / 10.0,
                severity, healthy, withIssues, top, history, recent);
    }

    @Transactional(readOnly = true)
    public AuditDtos.CompareDto compare(UserAccount user, Long firstId, Long secondId) {
        CrawlSession first = requireSession(user, firstId);
        CrawlSession second = requireSession(user, secondId);
        if (!first.getWebsite().getId().equals(second.getWebsite().getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Only two audits of the same website can be compared");
        }
        Map<String, Long> firstMap = countByType(first);
        Map<String, Long> secondMap = countByType(second);
        List<AuditDtos.IssueDelta> deltas = new ArrayList<>();
        firstMap.keySet().forEach(type -> {
            long a = firstMap.getOrDefault(type, 0L);
            long b = secondMap.getOrDefault(type, 0L);
            String trend = b < a ? "improved" : b > a ? "worsened" : "unchanged";
            deltas.add(new AuditDtos.IssueDelta(type, a, b, trend));
        });
        secondMap.keySet().stream().filter(type -> !firstMap.containsKey(type)).forEach(type ->
                deltas.add(new AuditDtos.IssueDelta(type, 0, secondMap.get(type), "worsened")));
        long fixed = deltas.stream().filter(d -> d.firstCount() > d.secondCount()).mapToLong(d -> d.firstCount() - d.secondCount()).sum();
        long newest = deltas.stream().filter(d -> d.secondCount() > d.firstCount()).mapToLong(d -> d.secondCount() - d.firstCount()).sum();
        long remaining = secondMap.values().stream().mapToLong(Long::longValue).sum();
        Map<String, Long> severity = new LinkedHashMap<>();
        for (IssueSeverity value : IssueSeverity.values()) {
            severity.put(value.name(), seoIssueRepository.countBySessionAndSeverity(second, value));
        }
        long pagesImproved = pagesImproved(first, second);
        return new AuditDtos.CompareDto(toSummary(first), toSummary(second), firstMap, secondMap, deltas,
                fixed, remaining, newest, pagesImproved, severity);
    }

    @Transactional
    public AuditDtos.IssueDto updateIssueStatus(UserAccount user, Long issueId, AuditDtos.UpdateIssueStatusRequest request) {
        if (request.getStatus() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Issue status is required");
        }
        SeoIssue issue = seoIssueRepository.findByIdAndSession_Owner(issueId, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Issue not found"));
        if (request.getStatus() == IssueStatus.FIXED) {
            issue.setStatus(IssueStatus.NEEDS_REVIEW);
        } else {
            issue.setStatus(request.getStatus());
        }
        return toIssue(seoIssueRepository.save(issue));
    }

    @Transactional
    public void delete(UserAccount user, Long id) {
        CrawlSession session = requireSession(user, id);
        seoIssueRepository.deleteBySession(session);
        crawledPageRepository.deleteBySession(session);
        crawlSessionRepository.delete(session);
    }

    public CrawlSession requireSession(UserAccount user, Long id) {
        return crawlSessionRepository.findByIdAndOwner(id, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Audit not found"));
    }

    public List<CrawledPage> pages(CrawlSession session) {
        return crawledPageRepository.findBySessionOrderByIdAsc(session);
    }

    public List<SeoIssue> issues(CrawlSession session) {
        return seoIssueRepository.findBySessionOrderBySeverityAscIdAsc(session);
    }

    private Map<String, Long> countByType(CrawlSession session) {
        Map<String, Long> map = new LinkedHashMap<>();
        seoIssueRepository.findBySessionOrderBySeverityAscIdAsc(session)
                .forEach(issue -> map.merge(issue.getIssueType().name(), 1L, Long::sum));
        return map;
    }

    private long pagesImproved(CrawlSession first, CrawlSession second) {
        Map<String, Long> firstCounts = new LinkedHashMap<>();
        seoIssueRepository.findBySessionOrderBySeverityAscIdAsc(first)
                .forEach(issue -> firstCounts.merge(issue.getPageUrl(), 1L, Long::sum));
        Map<String, Long> secondCounts = new LinkedHashMap<>();
        seoIssueRepository.findBySessionOrderBySeverityAscIdAsc(second)
                .forEach(issue -> secondCounts.merge(issue.getPageUrl(), 1L, Long::sum));
        return firstCounts.entrySet().stream()
                .filter(entry -> secondCounts.getOrDefault(entry.getKey(), 0L) < entry.getValue())
                .count();
    }

    public AuditDtos.AuditSummary toSummary(CrawlSession session) {
        return new AuditDtos.AuditSummary(
                session.getId(),
                session.getWebsite().getId(),
                session.getStartUrl(),
                session.getWebsite().getDomain(),
                session.getStatus(),
                session.getProgressPercent(),
                session.getProgressMessage(),
                session.getPagesCrawled(),
                session.getIssuesFound(),
                session.getHealthyPages(),
                session.getSeoScore(),
                session.getRobotsTxtAccessible(),
                session.getSitemapFound(),
                session.getSitemapValid(),
                session.getGa4TagDetected(),
                session.getErrorMessage(),
                session.getScoringBreakdown(),
                session.getStartedAt(),
                session.getFinishedAt(),
                session.getCreatedAt()
        );
    }

    private AuditDtos.PageDto toPage(CrawledPage page) {
        return new AuditDtos.PageDto(page.getId(), page.getUrl(), page.getStatusCode(), page.getRedirectHops(),
                page.getDepth(), page.getFetchTimeMs(), page.getTitle(), page.getMetaDescription(), page.getCanonical(),
                page.getH1Count(), page.getH1Text(), page.getImageCount(), page.getImagesMissingAlt(),
                page.getHasViewport(), page.getHttps(), page.getGa4TagDetected());
    }

    private AuditDtos.IssueDto toIssue(SeoIssue issue) {
        return new AuditDtos.IssueDto(issue.getId(), issue.getPage() == null ? null : issue.getPage().getId(),
                issue.getPageUrl(), issue.getIssueType(), issue.getSeverity(), issue.getTitle(),
                issue.getDescription(), issue.getRecommendation(), issue.getDetectedValue(), issue.getExpectedValue(),
                issue.getExplanation(), issue.getWhyItMatters(), issue.getHowToFix(), issue.getExampleSolution(),
                issue.getVerificationMethod(), issue.getStatus());
    }

    private int clamp(Integer value, int fallback, int min, int max) {
        int resolved = value == null ? fallback : value;
        return Math.max(min, Math.min(max, resolved));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
