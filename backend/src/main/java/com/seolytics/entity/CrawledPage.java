package com.seolytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "crawled_pages")
public class CrawledPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    private CrawlSession session;

    @Column(nullable = false, length = 2048)
    private String url;

    private Integer statusCode;
    private Integer redirectHops;
    private Integer depth;
    private Long fetchTimeMs;

    @Column(length = 512)
    private String title;

    @Column(length = 1024)
    private String metaDescription;

    @Column(length = 2048)
    private String canonical;

    private Integer h1Count;
    private Integer imageCount;
    private Integer imagesMissingAlt;
    private Boolean hasViewport;
    private Boolean https;
    private Boolean ga4TagDetected;
    private Boolean indexable;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String h1Text;

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Integer getRedirectHops() {
        return redirectHops;
    }

    public void setRedirectHops(Integer redirectHops) {
        this.redirectHops = redirectHops;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public Long getFetchTimeMs() {
        return fetchTimeMs;
    }

    public void setFetchTimeMs(Long fetchTimeMs) {
        this.fetchTimeMs = fetchTimeMs;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMetaDescription() {
        return metaDescription;
    }

    public void setMetaDescription(String metaDescription) {
        this.metaDescription = metaDescription;
    }

    public String getCanonical() {
        return canonical;
    }

    public void setCanonical(String canonical) {
        this.canonical = canonical;
    }

    public Integer getH1Count() {
        return h1Count;
    }

    public void setH1Count(Integer h1Count) {
        this.h1Count = h1Count;
    }

    public Integer getImageCount() {
        return imageCount;
    }

    public void setImageCount(Integer imageCount) {
        this.imageCount = imageCount;
    }

    public Integer getImagesMissingAlt() {
        return imagesMissingAlt;
    }

    public void setImagesMissingAlt(Integer imagesMissingAlt) {
        this.imagesMissingAlt = imagesMissingAlt;
    }

    public Boolean getHasViewport() {
        return hasViewport;
    }

    public void setHasViewport(Boolean hasViewport) {
        this.hasViewport = hasViewport;
    }

    public Boolean getHttps() {
        return https;
    }

    public void setHttps(Boolean https) {
        this.https = https;
    }

    public Boolean getGa4TagDetected() {
        return ga4TagDetected;
    }

    public void setGa4TagDetected(Boolean ga4TagDetected) {
        this.ga4TagDetected = ga4TagDetected;
    }

    public Boolean getIndexable() {
        return indexable;
    }

    public void setIndexable(Boolean indexable) {
        this.indexable = indexable;
    }

    public String getH1Text() {
        return h1Text;
    }

    public void setH1Text(String h1Text) {
        this.h1Text = h1Text;
    }
}
