package com.seolytics.crawler;

import com.seolytics.config.SeolyticsProperties;
import com.seolytics.domain.IssueSeverity;
import com.seolytics.entity.SeoIssue;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SeoScoreCalculator {

    private final SeolyticsProperties properties;

    public SeoScoreCalculator(SeolyticsProperties properties) {
        this.properties = properties;
    }

    public ScoreResult calculate(List<SeoIssue> issues) {
        Map<IssueSeverity, Long> uniqueTypesBySeverity = new EnumMap<>(IssueSeverity.class);
        Map<IssueSeverity, Long> occurrences = new EnumMap<>(IssueSeverity.class);
        for (IssueSeverity severity : IssueSeverity.values()) {
            uniqueTypesBySeverity.put(severity, 0L);
            occurrences.put(severity, 0L);
        }
        issues.stream()
                .collect(Collectors.groupingBy(SeoIssue::getSeverity))
                .forEach((severity, group) -> {
                    occurrences.put(severity, (long) group.size());
                    long unique = group.stream().map(SeoIssue::getIssueType).distinct().count();
                    uniqueTypesBySeverity.put(severity, unique);
                });

        SeolyticsProperties.Scoring scoring = properties.getScoring();
        double deduction = 0;
        StringBuilder breakdown = new StringBuilder("Start at 100. This is a SEOlytics technical health score, not Google's ranking algorithm. ");
        deduction += apply(uniqueTypesBySeverity, occurrences, IssueSeverity.CRITICAL, scoring.getCriticalBase(), scoring, breakdown);
        deduction += apply(uniqueTypesBySeverity, occurrences, IssueSeverity.HIGH, scoring.getHighBase(), scoring, breakdown);
        deduction += apply(uniqueTypesBySeverity, occurrences, IssueSeverity.MEDIUM, scoring.getMediumBase(), scoring, breakdown);
        deduction += apply(uniqueTypesBySeverity, occurrences, IssueSeverity.LOW, scoring.getLowBase(), scoring, breakdown);
        double score = Math.max(0, Math.min(100, Math.round((100 - deduction) * 10.0) / 10.0));
        breakdown.append("Final score: ").append(score).append("/100.");
        return new ScoreResult(score, breakdown.toString());
    }

    private double apply(Map<IssueSeverity, Long> unique, Map<IssueSeverity, Long> occurrences,
                         IssueSeverity severity, double base, SeolyticsProperties.Scoring scoring, StringBuilder breakdown) {
        long types = unique.getOrDefault(severity, 0L);
        long count = occurrences.getOrDefault(severity, 0L);
        if (types == 0) {
            return 0;
        }
        double extra = Math.min(scoring.getExtraOccurrenceCap(), Math.max(0, count - types) * scoring.getExtraOccurrence());
        double value = types * base + extra;
        breakdown.append(severity.name().toLowerCase(Locale.ROOT))
                .append(": -").append(round(value))
                .append(" (").append(types).append(" types, ").append(count).append(" occurrences). ");
        return value;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public record ScoreResult(double score, String breakdown) {
    }
}
