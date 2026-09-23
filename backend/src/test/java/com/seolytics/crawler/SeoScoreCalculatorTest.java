package com.seolytics.crawler;

import com.seolytics.config.SeolyticsProperties;
import com.seolytics.domain.IssueSeverity;
import com.seolytics.domain.IssueType;
import com.seolytics.entity.SeoIssue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeoScoreCalculatorTest {

    @Test
    void startsAtOneHundredWithoutIssues() {
        SeoScoreCalculator calculator = new SeoScoreCalculator(new SeolyticsProperties());
        SeoScoreCalculator.ScoreResult result = calculator.calculate(List.of());
        assertEquals(100.0, result.score());
        assertTrue(result.breakdown().contains("not Google"));
    }

    @Test
    void deductsForCriticalIssues() {
        SeoScoreCalculator calculator = new SeoScoreCalculator(new SeolyticsProperties());
        SeoIssue issue = new SeoIssue();
        issue.setIssueType(IssueType.MISSING_TITLE);
        issue.setSeverity(IssueSeverity.CRITICAL);
        SeoScoreCalculator.ScoreResult result = calculator.calculate(List.of(issue));
        assertTrue(result.score() < 100);
        assertTrue(result.score() >= 0);
    }
}
