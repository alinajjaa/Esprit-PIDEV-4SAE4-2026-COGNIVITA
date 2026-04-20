package com.alzheimer.medicalrecords;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;
import com.alzheimer.medicalrecords.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskTrendService Unit Tests")
class RiskTrendServiceTest {

    @Mock private RiskScoreHistoryRepository historyRepository;

    private RiskTrendService service;

    @BeforeEach
    void setUp() {
        service = new RiskTrendService(historyRepository);
    }

    @Test
    @DisplayName("Empty history returns NO_DATA status")
    void emptyHistory_returnsNoData() {
        when(historyRepository.findByRecordIdAsc(1L)).thenReturn(Collections.emptyList());
        Map<String, Object> report = service.getTrend(1L);
        assertThat(report.get("status")).isEqualTo("NO_DATA");
    }

    @Test
    @DisplayName("Single snapshot returns correct latestScore")
    void singleSnapshot_returnsLatestScore() {
        RiskScoreHistory snap = snapshot(35.0, RiskLevel.MEDIUM, LocalDateTime.now().minusDays(1));
        when(historyRepository.findByRecordIdAsc(1L)).thenReturn(List.of(snap));
        Map<String, Object> report = service.getTrend(1L);
        assertThat(report.get("latestScore")).isEqualTo(35.0);
    }

    @Test
    @DisplayName("Increasing scores → WORSENING trend")
    void increasingScores_worseningTrend() {
        List<RiskScoreHistory> snaps = List.of(
            snapshot(20.0, RiskLevel.MEDIUM, LocalDateTime.now().minusDays(30)),
            snapshot(30.0, RiskLevel.MEDIUM, LocalDateTime.now().minusDays(15)),
            snapshot(42.0, RiskLevel.MEDIUM, LocalDateTime.now())
        );
        when(historyRepository.findByRecordIdAsc(1L)).thenReturn(snaps);
        Map<String, Object> report = service.getTrend(1L);
        String direction = (String) report.get("trendDirection");
        assertThat(direction).isIn("WORSENING", "RAPIDLY_WORSENING");
    }

    @Test
    @DisplayName("Decreasing scores → IMPROVING trend")
    void decreasingScores_improvingTrend() {
        List<RiskScoreHistory> snaps = List.of(
            snapshot(55.0, RiskLevel.HIGH, LocalDateTime.now().minusDays(30)),
            snapshot(40.0, RiskLevel.MEDIUM, LocalDateTime.now().minusDays(15)),
            snapshot(28.0, RiskLevel.MEDIUM, LocalDateTime.now())
        );
        when(historyRepository.findByRecordIdAsc(1L)).thenReturn(snaps);
        Map<String, Object> report = service.getTrend(1L);
        assertThat(report.get("trendDirection")).isEqualTo("IMPROVING");
    }

    @Test
    @DisplayName("Stable scores → STABLE trend")
    void stableScores_stableTrend() {
        List<RiskScoreHistory> snaps = List.of(
            snapshot(30.0, RiskLevel.MEDIUM, LocalDateTime.now().minusDays(30)),
            snapshot(30.5, RiskLevel.MEDIUM, LocalDateTime.now().minusDays(15)),
            snapshot(30.2, RiskLevel.MEDIUM, LocalDateTime.now())
        );
        when(historyRepository.findByRecordIdAsc(1L)).thenReturn(snaps);
        Map<String, Object> report = service.getTrend(1L);
        assertThat(report.get("trendDirection")).isEqualTo("STABLE");
    }

    @Test
    @DisplayName("Report contains projections when ≥ 2 data points")
    void multipleSnapshots_containsProjections() {
        List<RiskScoreHistory> snaps = List.of(
            snapshot(20.0, RiskLevel.LOW,    LocalDateTime.now().minusDays(30)),
            snapshot(35.0, RiskLevel.MEDIUM, LocalDateTime.now())
        );
        when(historyRepository.findByRecordIdAsc(1L)).thenReturn(snaps);
        Map<String, Object> report = service.getTrend(1L);
        assertThat(report).containsKey("projections");
        @SuppressWarnings("unchecked")
        Map<String, Object> proj = (Map<String, Object>) report.get("projections");
        assertThat(proj).containsKeys("threeMonths", "sixMonths", "twelveMonths", "confidence");
    }

    @Test
    @DisplayName("Projected scores are always 0–99")
    void projections_alwaysInRange() {
        List<RiskScoreHistory> snaps = List.of(
            snapshot(5.0,  RiskLevel.LOW,      LocalDateTime.now().minusDays(10)),
            snapshot(90.0, RiskLevel.CRITICAL,  LocalDateTime.now())
        );
        when(historyRepository.findByRecordIdAsc(1L)).thenReturn(snaps);
        Map<String, Object> report = service.getTrend(1L);
        @SuppressWarnings("unchecked")
        Map<String, Object> proj = (Map<String, Object>) report.get("projections");
        for (String key : List.of("threeMonths", "sixMonths", "twelveMonths")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> pt = (Map<String, Object>) proj.get(key);
            double s = ((Number) pt.get("score")).doubleValue();
            assertThat(s).isBetween(0.0, 99.0);
        }
    }

    @Test
    @DisplayName("Chart data has required keys per data point")
    void chartData_hasRequiredKeys() {
        List<RiskScoreHistory> snaps = List.of(
            snapshot(25.0, RiskLevel.MEDIUM, LocalDateTime.now())
        );
        when(historyRepository.findByRecordIdAsc(1L)).thenReturn(snaps);
        List<Map<String, Object>> chartData = service.getChartData(1L);
        assertThat(chartData).hasSize(1);
        assertThat(chartData.get(0)).containsKeys("date", "score", "riskLevel", "triggerReason");
    }

    @Test
    @DisplayName("Report contains totalChange (latestScore - firstScore)")
    void report_containsTotalChange() {
        List<RiskScoreHistory> snaps = List.of(
            snapshot(20.0, RiskLevel.LOW,  LocalDateTime.now().minusDays(30)),
            snapshot(45.0, RiskLevel.HIGH, LocalDateTime.now())
        );
        when(historyRepository.findByRecordIdAsc(1L)).thenReturn(snaps);
        Map<String, Object> report = service.getTrend(1L);
        assertThat((Double) report.get("totalChange")).isEqualTo(25.0);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private RiskScoreHistory snapshot(double score, RiskLevel level, LocalDateTime at) {
        RiskScoreHistory h = new RiskScoreHistory();
        h.setRecordId(1L);
        h.setScore(score);
        h.setRiskLevel(level);
        h.setCalculatedAt(at);
        h.setTriggerReason("TEST");
        h.setHereditaryContribution(0.0);
        h.setWellnessContribution(0.0);
        return h;
    }
}
