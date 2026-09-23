package com.seolytics.entity;

import com.seolytics.domain.IssueSeverity;
import com.seolytics.domain.IssueStatus;
import com.seolytics.domain.IssueType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "seo_issues")
public class SeoIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    private CrawlSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id")
    private CrawledPage page;

    @Column(length = 2048)
    private String pageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private IssueType issueType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private IssueSeverity severity;

    @Column(nullable = false, length = 255)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @Column(length = 2048)
    private String detectedValue;

    @Column(length = 2048)
    private String expectedValue;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String whyItMatters;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String howToFix;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String exampleSolution;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String verificationMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private IssueStatus status = IssueStatus.OPEN;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CrawlSession getSession() {
        return session;
    }

    public void setSession(CrawlSession session) {
        this.session = session;
    }

    public CrawledPage getPage() {
        return page;
    }

    public void setPage(CrawledPage page) {
        this.page = page;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public IssueType getIssueType() {
        return issueType;
    }

    public void setIssueType(IssueType issueType) {
        this.issueType = issueType;
    }

    public IssueSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(IssueSeverity severity) {
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getDetectedValue() {
        return detectedValue;
    }

    public void setDetectedValue(String detectedValue) {
        this.detectedValue = detectedValue;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getWhyItMatters() {
        return whyItMatters;
    }

    public void setWhyItMatters(String whyItMatters) {
        this.whyItMatters = whyItMatters;
    }

    public String getHowToFix() {
        return howToFix;
    }

    public void setHowToFix(String howToFix) {
        this.howToFix = howToFix;
    }

    public String getExampleSolution() {
        return exampleSolution;
    }

    public void setExampleSolution(String exampleSolution) {
        this.exampleSolution = exampleSolution;
    }

    public String getVerificationMethod() {
        return verificationMethod;
    }

    public void setVerificationMethod(String verificationMethod) {
        this.verificationMethod = verificationMethod;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public void setStatus(IssueStatus status) {
        this.status = status;
    }
}
