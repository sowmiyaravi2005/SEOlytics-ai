package com.seolytics.entity;

import com.seolytics.domain.CrawlStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "crawl_sessions")
public class CrawlSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "website_id")
    private Website website;

    @Column(nullable = false, length = 2048)
    private String startUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CrawlStatus status = CrawlStatus.PENDING;

    private int maxPages;
    private int maxDepth;
    private int pagesCrawled;
    private int issuesFound;
    private int healthyPages;
    private Double seoScore;

    @Column(length = 500)
    private String progressMessage;

    private Integer progressPercent;
    private Boolean robotsTxtAccessible;
    private Boolean sitemapFound;
    private Boolean sitemapValid;
    private Boolean ga4TagDetected;

    @Column(length = 1000)
    private String errorMessage;

    @Column(length = 2000)
    private String scoringBreakdown;

    private Instant startedAt;
    private Instant finishedAt;
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = CrawlStatus.PENDING;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserAccount getOwner() {
        return owner;
    }

    public void setOwner(UserAccount owner) {
        this.owner = owner;
    }

    public Website getWebsite() {
        return website;
    }

    public void setWebsite(Website website) {
        this.website = website;
    }

    public String getStartUrl() {
        return startUrl;
    }

    public void setStartUrl(String startUrl) {
        this.startUrl = startUrl;
    }

    public CrawlStatus getStatus() {
        return status;
    }

    public void setStatus(CrawlStatus status) {
        this.status = status;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public int getPagesCrawled() {
        return pagesCrawled;
    }

    public void setPagesCrawled(int pagesCrawled) {
        this.pagesCrawled = pagesCrawled;
    }

    public int getIssuesFound() {
        return issuesFound;
    }

    public void setIssuesFound(int issuesFound) {
        this.issuesFound = issuesFound;
    }

    public int getHealthyPages() {
        return healthyPages;
    }

    public void setHealthyPages(int healthyPages) {
        this.healthyPages = healthyPages;
    }

    public Double getSeoScore() {
        return seoScore;
    }

    public void setSeoScore(Double seoScore) {
        this.seoScore = seoScore;
    }

    public String getProgressMessage() {
        return progressMessage;
    }

    public void setProgressMessage(String progressMessage) {
        this.progressMessage = progressMessage;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    public Boolean getRobotsTxtAccessible() {
        return robotsTxtAccessible;
    }

    public void setRobotsTxtAccessible(Boolean robotsTxtAccessible) {
        this.robotsTxtAccessible = robotsTxtAccessible;
    }

    public Boolean getSitemapFound() {
        return sitemapFound;
    }

    public void setSitemapFound(Boolean sitemapFound) {
        this.sitemapFound = sitemapFound;
    }

    public Boolean getSitemapValid() {
        return sitemapValid;
    }

    public void setSitemapValid(Boolean sitemapValid) {
        this.sitemapValid = sitemapValid;
    }

    public Boolean getGa4TagDetected() {
        return ga4TagDetected;
    }

    public void setGa4TagDetected(Boolean ga4TagDetected) {
        this.ga4TagDetected = ga4TagDetected;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getScoringBreakdown() {
        return scoringBreakdown;
    }

    public void setScoringBreakdown(String scoringBreakdown) {
        this.scoringBreakdown = scoringBreakdown;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
