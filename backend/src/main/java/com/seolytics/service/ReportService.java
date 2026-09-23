package com.seolytics.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.seolytics.entity.CrawledPage;
import com.seolytics.entity.CrawlSession;
import com.seolytics.entity.SeoIssue;
import com.seolytics.entity.UserAccount;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneId.of("UTC"));

    private final AuditService auditService;

    public ReportService(AuditService auditService) {
        this.auditService = auditService;
    }

    public byte[] htmlReport(UserAccount user, Long auditId) {
        return buildHtml(user, auditId).getBytes(StandardCharsets.UTF_8);
    }

    public byte[] pdfReport(UserAccount user, Long auditId) {
        String html = buildHtml(user, auditId);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate PDF report", ex);
        }
    }

    private String buildHtml(UserAccount user, Long auditId) {
        CrawlSession session = auditService.requireSession(user, auditId);
        List<CrawledPage> pages = auditService.pages(session);
        List<SeoIssue> issues = auditService.issues(session);
        List<SeoIssue> priority = issues.stream()
                .filter(i -> i.getSeverity().name().equals("CRITICAL") || i.getSeverity().name().equals("HIGH"))
                .toList();
        String issueRows = issues.stream().map(issue -> "<tr>"
                + td(issue.getSeverity().name())
                + td(issue.getStatus() == null ? "OPEN" : issue.getStatus().name())
                + td(issue.getIssueType().name())
                + td(esc(issue.getPageUrl()))
                + td(esc(issue.getTitle()))
                + td(esc(issue.getWhyItMatters()))
                + td(esc(issue.getHowToFix() == null ? issue.getRecommendation() : issue.getHowToFix()))
                + td(esc(issue.getExampleSolution()))
                + td(esc(issue.getVerificationMethod()))
                + "</tr>").collect(Collectors.joining());
        String pageRows = pages.stream().limit(80).map(page -> "<tr>"
                + td(esc(page.getUrl()))
                + td(String.valueOf(page.getStatusCode()))
                + td(esc(page.getTitle()))
                + td(esc(page.getMetaDescription()))
                + td(esc(page.getCanonical()))
                + td(String.valueOf(page.getH1Count()))
                + "</tr>").collect(Collectors.joining());
        String priorityRows = priority.stream().map(issue -> "<tr>"
                + td(issue.getSeverity().name())
                + td(esc(issue.getTitle()))
                + td(esc(issue.getPageUrl()))
                + td(esc(issue.getHowToFix() == null ? issue.getRecommendation() : issue.getHowToFix()))
                + "</tr>").collect(Collectors.joining());

        return """
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head>
                  <meta charset="UTF-8"/>
                  <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; color: #0f172a; margin: 32px; }
                    h1 { color: #1d4ed8; margin-bottom: 4px; }
                    h2 { color: #1e3a8a; margin-top: 28px; }
                    .meta { color: #475569; margin-bottom: 16px; }
                    .score { font-size: 28px; font-weight: 700; color: #0f766e; }
                    table { width: 100%%; border-collapse: collapse; margin-top: 10px; font-size: 11px; }
                    th { background: #eff6ff; text-align: left; padding: 8px; border-bottom: 1px solid #bfdbfe; }
                    td { padding: 7px 8px; border-bottom: 1px solid #e2e8f0; vertical-align: top; }
                    .note { font-size: 11px; color: #64748b; }
                  </style>
                </head>
                <body>
                  <h1>SEOlytics Technical SEO Audit</h1>
                  <div class="meta">Prepared for %s · %s</div>
                  <p><strong>Website:</strong> %s</p>
                  <p class="score">SEO health score: %s / 100</p>
                  <p class="note">%s</p>
                  <h2>Crawl summary</h2>
                  <p>Pages crawled: %s · Issues found: %s · Healthy pages: %s · Status: %s</p>
                  <p>robots.txt accessible: %s · sitemap found: %s · GA4 snippet detected: %s</p>
                  <p class="note">GA4 detection only confirms a common tag or measurement ID in HTML. It does not verify that analytics data collection is working.</p>
                  <h2>Critical and high-priority issues</h2>
                  <table><thead><tr><th>Severity</th><th>Issue</th><th>URL</th><th>How to fix</th></tr></thead>
                  <tbody>%s</tbody></table>
                  <h2>SEO Fix Center Export</h2>
                  <p class="note">Suggested fixes are guidance only. SEOlytics does not modify websites automatically; re-crawl after applying changes to verify.</p>
                  <table><thead><tr><th>Severity</th><th>Status</th><th>Type</th><th>URL</th><th>Issue</th><th>Why it matters</th><th>How to fix</th><th>Example</th><th>Verify</th></tr></thead>
                  <tbody>%s</tbody></table>
                  <h2>Page-level metadata</h2>
                  <table><thead><tr><th>URL</th><th>Status</th><th>Title</th><th>Meta description</th><th>Canonical</th><th>H1s</th></tr></thead>
                  <tbody>%s</tbody></table>
                </body>
                </html>
                """.formatted(
                esc(user.getFullName()),
                session.getFinishedAt() == null ? DATE_FORMAT.format(session.getCreatedAt()) : DATE_FORMAT.format(session.getFinishedAt()),
                esc(session.getStartUrl()),
                session.getSeoScore() == null ? "—" : session.getSeoScore(),
                esc(session.getScoringBreakdown() == null ? "Score starts at 100 and deducts points by severity. This is not Google's ranking algorithm." : session.getScoringBreakdown()),
                session.getPagesCrawled(),
                session.getIssuesFound(),
                session.getHealthyPages(),
                session.getStatus(),
                session.getRobotsTxtAccessible(),
                session.getSitemapFound(),
                session.getGa4TagDetected(),
                priorityRows.isBlank() ? "<tr><td colspan='4'>None</td></tr>" : priorityRows,
                issueRows.isBlank() ? "<tr><td colspan='9'>None</td></tr>" : issueRows,
                pageRows.isBlank() ? "<tr><td colspan='6'>None</td></tr>" : pageRows
        );
    }

    private String td(String value) {
        return "<td>" + (value == null ? "" : value) + "</td>";
    }

    private String esc(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
