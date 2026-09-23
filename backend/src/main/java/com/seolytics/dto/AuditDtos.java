package com.seolytics.dto;

import com.seolytics.domain.CrawlStatus;
import com.seolytics.domain.IssueSeverity;
import com.seolytics.domain.IssueStatus;
import com.seolytics.domain.IssueType;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class AuditDtos {

    public static class StartAuditRequest {
        @NotBlank
        private String url;
        private Integer maxPages;
        private Integer maxDepth;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Integer getMaxPages() {
            return maxPages;
        }

        public void setMaxPages(Integer maxPages) {
            this.maxPages = maxPages;
        }

        public Integer getMaxDepth() {
            return maxDepth;
        }

        public void setMaxDepth(Integer maxDepth) {
            this.maxDepth = maxDepth;
        }
    }

    public record AuditSummary(
            Long id,
            Long websiteId,
            String url,
            String domain,
            CrawlStatus status,
            Integer progressPercent,
            String progressMessage,
            Integer pagesCrawled,
            Integer issuesFound,
            Integer healthyPages,
            Double seoScore,
            Boolean robotsTxtAccessible,
            Boolean sitemapFound,
            Boolean sitemapValid,
            Boolean ga4TagDetected,
            String errorMessage,
            String scoringBreakdown,
            Instant startedAt,
            Instant finishedAt,
            Instant createdAt
    ) {
    }

    public record PageDto(
            Long id,
            String url,
            Integer statusCode,
            Integer redirectHops,
            Integer depth,
            Long fetchTimeMs,
            String title,
            String metaDescription,
            String canonical,
            Integer h1Count,
            String h1Text,
            Integer imageCount,
            Integer imagesMissingAlt,
            Boolean hasViewport,
            Boolean https,
            Boolean ga4TagDetected
    ) {
    }

    public record IssueDto(
            Long id,
            Long pageId,
            String pageUrl,
            IssueType issueType,
            IssueSeverity severity,
            String title,
            String description,
            String recommendation,
            String detectedValue,
            String expectedValue,
            String explanation,
            String whyItMatters,
            String howToFix,
            String exampleSolution,
            String verificationMethod,
            IssueStatus status
    ) {
    }

    public static class UpdateIssueStatusRequest {
        private IssueStatus status;

        public IssueStatus getStatus() {
            return status;
        }

        public void setStatus(IssueStatus status) {
            this.status = status;
        }
    }

    public record AuditDetail(
            AuditSummary summary,
            Map<String, Long> issuesBySeverity,
            List<PageDto> pages,
            List<IssueDto> issues
    ) {
    }

    public record ProgressDto(
            Long id,
            CrawlStatus status,
            Integer progressPercent,
            String progressMessage,
            Integer pagesCrawled,
            Integer maxPages,
            String errorMessage
    ) {
    }

    public record DashboardDto(
            long totalWebsites,
            long totalAudits,
            long totalPagesCrawled,
            Double overallSeoScore,
            Map<String, Long> latestSeverityCounts,
            long healthyPages,
            long pagesWithIssues,
            List<IssueCount> topIssues,
            List<HistoryPoint> crawlHistory,
            List<AuditSummary> recentAudits
    ) {
    }

    public record IssueCount(String issueType, long count) {
    }

    public record HistoryPoint(Instant date, Double score, Integer pagesCrawled, Integer issuesFound) {
    }

    public record CompareDto(
            AuditSummary first,
            AuditSummary second,
            Map<String, Long> firstByType,
            Map<String, Long> secondByType,
            List<IssueDelta> deltas,
            long issuesFixed,
            long remainingIssues,
            long newIssues,
            long pagesImproved,
            Map<String, Long> secondBySeverity
    ) {
    }

    public record IssueDelta(String issueType, long firstCount, long secondCount, String trend) {
    }
}
