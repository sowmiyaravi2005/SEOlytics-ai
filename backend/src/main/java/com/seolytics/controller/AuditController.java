package com.seolytics.controller;

import com.seolytics.domain.IssueSeverity;
import com.seolytics.domain.IssueType;
import com.seolytics.dto.AuditDtos;
import com.seolytics.entity.UserAccount;
import com.seolytics.service.AuditService;
import com.seolytics.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AuditController {

    private final AuditService auditService;
    private final ReportService reportService;

    public AuditController(AuditService auditService, ReportService reportService) {
        this.auditService = auditService;
        this.reportService = reportService;
    }

    @PostMapping("/api/audits")
    public AuditDtos.AuditSummary start(@AuthenticationPrincipal UserAccount user,
                                        @Valid @RequestBody AuditDtos.StartAuditRequest request) {
        return auditService.startAudit(user, request);
    }

    @PostMapping("/api/audits/{id}/recrawl")
    public AuditDtos.AuditSummary recrawl(@AuthenticationPrincipal UserAccount user, @PathVariable Long id) {
        return auditService.recrawl(user, id);
    }

    @GetMapping("/api/audits")
    public List<AuditDtos.AuditSummary> list(@AuthenticationPrincipal UserAccount user) {
        return auditService.list(user);
    }

    @GetMapping("/api/audits/{id}")
    public AuditDtos.AuditDetail detail(@AuthenticationPrincipal UserAccount user,
                                        @PathVariable Long id,
                                        @RequestParam(required = false) String query,
                                        @RequestParam(required = false) IssueSeverity severity,
                                        @RequestParam(required = false) IssueType type,
                                        @RequestParam(required = false) Integer status) {
        return auditService.detail(user, id, query, severity, type, status);
    }

    @GetMapping("/api/audits/{id}/progress")
    public AuditDtos.ProgressDto progress(@AuthenticationPrincipal UserAccount user, @PathVariable Long id) {
        return auditService.progress(user, id);
    }

    @GetMapping("/api/audits/{id}/compare/{otherId}")
    public AuditDtos.CompareDto compare(@AuthenticationPrincipal UserAccount user,
                                        @PathVariable Long id,
                                        @PathVariable Long otherId) {
        return auditService.compare(user, id, otherId);
    }

    @DeleteMapping("/api/audits/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserAccount user, @PathVariable Long id) {
        auditService.delete(user, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/issues/{issueId}/status")
    public AuditDtos.IssueDto updateIssueStatus(@AuthenticationPrincipal UserAccount user,
                                                @PathVariable Long issueId,
                                                @RequestBody AuditDtos.UpdateIssueStatusRequest request) {
        return auditService.updateIssueStatus(user, issueId, request);
    }

    @GetMapping("/api/pages/{pageId}")
    public AuditDtos.PageDto page(@AuthenticationPrincipal UserAccount user, @PathVariable Long pageId) {
        return auditService.page(user, pageId);
    }

    @GetMapping("/api/dashboard")
    public AuditDtos.DashboardDto dashboard(@AuthenticationPrincipal UserAccount user) {
        return auditService.dashboard(user);
    }

    @GetMapping("/api/audits/{id}/report.html")
    public ResponseEntity<byte[]> html(@AuthenticationPrincipal UserAccount user, @PathVariable Long id) {
        byte[] body = reportService.htmlReport(user, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"seolytics-audit-" + id + ".html\"")
                .contentType(MediaType.TEXT_HTML)
                .body(body);
    }

    @GetMapping("/api/audits/{id}/report.pdf")
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal UserAccount user, @PathVariable Long id) {
        byte[] body = reportService.pdfReport(user, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"seolytics-audit-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(body);
    }
}
