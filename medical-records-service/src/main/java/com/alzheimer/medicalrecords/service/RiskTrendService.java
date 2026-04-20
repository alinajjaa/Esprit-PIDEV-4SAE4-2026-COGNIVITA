package com.alzheimer.medicalrecords.service;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Analyses historical risk score snapshots to:
 *  - Show a trend over time (for chart rendering)
 *  - Project future score at 3, 6, 12 months using linear regression
 *  - Detect trend direction (IMPROVING / STABLE / WORSENING / RAPIDLY_WORSENING)
 *  - Identify what is driving the change
 */
@Service
public class RiskTrendService {

    private final RiskScoreHistoryRepository historyRepository;

    public RiskTrendService(RiskScoreHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    // ── Main API ──────────────────────────────────────────────────────────────

    /**
     * Full trend report for a medical record.
     */
    public Map<String, Object> getTrend(Long recordId) {
        List<RiskScoreHistory> history = historyRepository.findByRecordIdAsc(recordId);
        Map<String, Object> report = new LinkedHashMap<>();

        if (history.isEmpty()) {
            report.put("status", "NO_DATA");
            report.put("message", "No risk score history found. Score history is recorded each time the record is updated.");
            return report;
        }

        // Data points for chart
        List<Map<String, Object>> dataPoints = buildDataPoints(history);
        report.put("dataPoints", dataPoints);
        report.put("totalSnapshots", history.size());

        // First and latest for comparison
        double firstScore  = history.get(0).getScore();
        double latestScore = history.get(history.size() - 1).getScore();
        double change      = latestScore - firstScore;
        report.put("firstScore",  round(firstScore));
        report.put("latestScore", round(latestScore));
        report.put("totalChange", round(change));
        report.put("firstDate",   history.get(0).getCalculatedAt());
        report.put("latestDate",  history.get(history.size() - 1).getCalculatedAt());

        // Trend direction
        String direction = trendDirection(history);
        report.put("trendDirection", direction);
        report.put("trendLabel",     trendLabel(direction));
        report.put("trendColor",     trendColor(direction));

        // 30-day velocity (pts/day)
        double velocity = computeVelocity(history);
        report.put("velocityPerDay",  round(velocity));
        report.put("velocityPerMonth", round(velocity * 30));

        // Projections — only meaningful if we have ≥ 2 data points
        if (history.size() >= 2) {
            Map<String, Object> projections = project(history, velocity);
            report.put("projections", projections);
        }

        // Risk level distribution across history
        report.put("riskLevelHistory", riskLevelDistribution(history));

        // Longest streak at current level
        report.put("currentStreak", currentStreak(history));

        return report;
    }

    /**
     * Lightweight version — just the data points (for charting without full analytics).
     */
    public List<Map<String, Object>> getChartData(Long recordId) {
        return buildDataPoints(historyRepository.findByRecordIdAsc(recordId));
    }

    // ── Projection ────────────────────────────────────────────────────────────

    private Map<String, Object> project(List<RiskScoreHistory> history, double velocityPerDay) {
        double latest = history.get(history.size() - 1).getScore();

        // Simple linear extrapolation from the last 30-day velocity
        double at3m  = clamp(latest + velocityPerDay * 30  * 3);
        double at6m  = clamp(latest + velocityPerDay * 30  * 6);
        double at12m = clamp(latest + velocityPerDay * 30 * 12);

        Map<String, Object> p = new LinkedHashMap<>();
        p.put("threeMonths",   projPoint(at3m,  3));
        p.put("sixMonths",     projPoint(at6m,  6));
        p.put("twelveMonths",  projPoint(at12m, 12));

        // Confidence: higher with more data points, lower with high variance
        double variance = computeVariance(history);
        String confidence = history.size() >= 10 && variance < 25 ? "HIGH"
                          : history.size() >= 5                    ? "MEDIUM"
                          :                                           "LOW";
        p.put("confidence", confidence);
        p.put("note",       buildProjectionNote(latest, velocityPerDay, confidence));
        return p;
    }

    private Map<String, Object> projPoint(double score, int months) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("score",     round(score));
        p.put("riskLevel", levelName(score));
        p.put("months",    months);
        p.put("color",     scoreColor(score));
        return p;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Map<String, Object>> buildDataPoints(List<RiskScoreHistory> history) {
        List<Map<String, Object>> pts = new ArrayList<>();
        for (RiskScoreHistory h : history) {
            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("date",         h.getCalculatedAt());
            pt.put("score",        round(h.getScore()));
            pt.put("riskLevel",    h.getRiskLevel() != null ? h.getRiskLevel().name() : "LOW");
            pt.put("triggerReason", h.getTriggerReason());
            pt.put("hereditary",   h.getHereditaryContribution());
            pt.put("wellness",     h.getWellnessContribution());
            pts.add(pt);
        }
        return pts;
    }

    /** Velocity in points per day based on the last 30-day window (or full history if shorter) */
    private double computeVelocity(List<RiskScoreHistory> history) {
        if (history.size() < 2) return 0;
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        // Find the oldest point in the 30-day window
        int startIdx = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).getCalculatedAt().isBefore(cutoff)) {
                startIdx = i;
                break;
            }
        }
        RiskScoreHistory start = history.get(startIdx);
        RiskScoreHistory end   = history.get(history.size() - 1);
        long days = ChronoUnit.DAYS.between(start.getCalculatedAt(), end.getCalculatedAt());
        if (days == 0) return 0;
        return (end.getScore() - start.getScore()) / days;
    }

    private double computeVariance(List<RiskScoreHistory> history) {
        double mean = history.stream().mapToDouble(RiskScoreHistory::getScore).average().orElse(0);
        return history.stream().mapToDouble(h -> Math.pow(h.getScore() - mean, 2)).average().orElse(0);
    }

    private String trendDirection(List<RiskScoreHistory> history) {
        if (history.size() < 2) return "STABLE";
        double velocity = computeVelocity(history);
        if      (velocity >  0.5) return "RAPIDLY_WORSENING";
        else if (velocity >  0.1) return "WORSENING";
        else if (velocity < -0.1) return "IMPROVING";
        else                      return "STABLE";
    }

    private String trendLabel(String direction) {
        return switch (direction) {
            case "RAPIDLY_WORSENING" -> "⬆️ Rapidly Worsening";
            case "WORSENING"         -> "↗️ Worsening";
            case "IMPROVING"         -> "↘️ Improving";
            default                  -> "→ Stable";
        };
    }

    private String trendColor(String direction) {
        return switch (direction) {
            case "RAPIDLY_WORSENING" -> "#dc2626";
            case "WORSENING"         -> "#f97316";
            case "IMPROVING"         -> "#10b981";
            default                  -> "#64748b";
        };
    }

    private Map<String, Long> riskLevelDistribution(List<RiskScoreHistory> history) {
        Map<String, Long> dist = new LinkedHashMap<>();
        dist.put("LOW",      history.stream().filter(h -> h.getRiskLevel() == RiskLevel.LOW).count());
        dist.put("MEDIUM",   history.stream().filter(h -> h.getRiskLevel() == RiskLevel.MEDIUM).count());
        dist.put("HIGH",     history.stream().filter(h -> h.getRiskLevel() == RiskLevel.HIGH).count());
        dist.put("CRITICAL", history.stream().filter(h -> h.getRiskLevel() == RiskLevel.CRITICAL).count());
        return dist;
    }

    private int currentStreak(List<RiskScoreHistory> history) {
        if (history.isEmpty()) return 0;
        RiskLevel last = history.get(history.size() - 1).getRiskLevel();
        int streak = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).getRiskLevel() == last) streak++;
            else break;
        }
        return streak;
    }

    private String buildProjectionNote(double current, double velocity, String confidence) {
        if (Math.abs(velocity) < 0.05) return "Risk score is stable. No significant change expected.";
        if (velocity > 0)
            return String.format("Score is increasing by ~%.1f pts/month. Based on %s-confidence projection.",
                    velocity * 30, confidence);
        return String.format("Score is improving by ~%.1f pts/month. Based on %s-confidence projection.",
                Math.abs(velocity) * 30, confidence);
    }

    private String levelName(double score) {
        if (score < 20) return "LOW";
        if (score < 45) return "MEDIUM";
        if (score < 70) return "HIGH";
        return "CRITICAL";
    }

    private String scoreColor(double score) {
        if (score < 20) return "#10b981";
        if (score < 45) return "#f59e0b";
        if (score < 70) return "#f97316";
        return "#dc2626";
    }

    private double clamp(double v) { return Math.max(0, Math.min(99, v)); }
    private double round(double v) { return Math.round(v * 10.0) / 10.0; }
}
