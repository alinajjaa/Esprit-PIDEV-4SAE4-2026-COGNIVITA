package com.alzheimer.medicalrecords.controller;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;
import com.alzheimer.medicalrecords.service.*;
import com.alzheimer.medicalrecords.user.*;
import com.alzheimer.medicalrecords.dto.*;

import com.alzheimer.medicalrecords.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.List;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final MedicalRecordRepository medicalRecordRepository;
    private final RiskFactorRepository riskFactorRepository;
    private final PreventionActionRepository preventionActionRepository;
    private final TimelineRepository timelineRepository;

    public StatisticsController(MedicalRecordRepository medicalRecordRepository,
                                RiskFactorRepository riskFactorRepository,
                                PreventionActionRepository preventionActionRepository,
                                TimelineRepository timelineRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.riskFactorRepository = riskFactorRepository;
        this.preventionActionRepository = preventionActionRepository;
        this.timelineRepository = timelineRepository;
    }

    @GetMapping("/medical-record/{medicalRecordId}/risk-evolution")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRiskEvolution(
            @PathVariable Long medicalRecordId) {
        try {
            Optional<MedicalRecord> recordOpt = medicalRecordRepository.findById(medicalRecordId);
            if (recordOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Record not found", "No record with ID: " + medicalRecordId));
            }
            MedicalRecord record = recordOpt.get();
            List<MedicalTimeline> events = timelineRepository
                    .findByMedicalRecordIdOrderByEventDateDesc(medicalRecordId);

            List<Map<String, Object>> dataPoints = new ArrayList<>();
            double currentScore = record.getRiskScore() != null ? record.getRiskScore() : 0;

            dataPoints.add(Map.of(
                "date", record.getLastRiskCalculation() != null
                    ? record.getLastRiskCalculation().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "score", currentScore,
                "label", "Current"
            ));

            Collections.reverse(events);
            double simulatedScore = currentScore > 20 ? currentScore - 10 : currentScore;
            for (int i = 0; i < Math.min(events.size(), 10); i++) {
                MedicalTimeline event = events.get(i);
                dataPoints.add(0, Map.of(
                    "date", event.getEventDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    "score", simulatedScore,
                    "label", event.getEventType().name()
                ));
                simulatedScore = Math.max(0, simulatedScore - 5);
            }

            List<String> labels = dataPoints.stream()
                .map(p -> LocalDateTime.parse(p.get("date").toString())
                    .format(DateTimeFormatter.ofPattern("MMM dd")))
                .collect(Collectors.toList());
            List<Double> scores = dataPoints.stream()
                .map(p -> (Double) p.get("score"))
                .collect(Collectors.toList());

            Map<String, Object> chartData = new LinkedHashMap<>();
            chartData.put("labels", labels);
            chartData.put("scores", scores);
            chartData.put("dataPoints", dataPoints);

            return ResponseEntity.ok(ApiResponse.success("Risk evolution retrieved", chartData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve risk evolution", e.getMessage()));
        }
    }

    @GetMapping("/medical-record/{medicalRecordId}/actions-per-month")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActionsPerMonth(
            @PathVariable Long medicalRecordId) {
        try {
            // JPQL-based grouping
            List<Object[]> rows = preventionActionRepository.countActionsByMonthForRecord(medicalRecordId);

            List<String> months = new ArrayList<>();
            List<Long> counts = new ArrayList<>();
            for (Object[] row : rows) {
                months.add(row[0].toString());
                counts.add(((Number) row[1]).longValue());
            }

            Long total = preventionActionRepository.countByMedicalRecordId(medicalRecordId);

            Map<String, Object> chartData = new LinkedHashMap<>();
            chartData.put("labels", months);
            chartData.put("counts", counts);
            chartData.put("total", total);

            return ResponseEntity.ok(ApiResponse.success("Actions per month retrieved", chartData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve actions per month", e.getMessage()));
        }
    }

    @GetMapping("/medical-record/{medicalRecordId}/risk-factors-distribution")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRiskFactorsDistribution(
            @PathVariable Long medicalRecordId) {
        try {
            // JPQL grouping by severity
            List<Object[]> rows = riskFactorRepository.countActiveBySeverityForRecord(medicalRecordId);

            Map<String, Long> distribution = new LinkedHashMap<>();
            for (Object[] row : rows) {
                distribution.put(row[0].toString(), ((Number) row[1]).longValue());
            }

            List<String> labels = Arrays.asList("CRITICAL", "HIGH", "MEDIUM", "LOW");
            List<Long> counts = labels.stream()
                .map(label -> distribution.getOrDefault(label, 0L))
                .collect(Collectors.toList());

            Long total = riskFactorRepository.countByMedicalRecordId(medicalRecordId);

            Map<String, Object> chartData = new LinkedHashMap<>();
            chartData.put("labels", labels);
            chartData.put("counts", counts);
            chartData.put("total", total);

            return ResponseEntity.ok(ApiResponse.success("Distribution retrieved", chartData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve distribution", e.getMessage()));
        }
    }

    @GetMapping("/medical-record/{medicalRecordId}/adherence")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdherenceStats(
            @PathVariable Long medicalRecordId) {
        try {
            Long total = preventionActionRepository.countByMedicalRecordId(medicalRecordId);
            Long completed = preventionActionRepository.countByMedicalRecordIdAndStatus(
                medicalRecordId, ActionStatus.COMPLETED);
            Long pending = preventionActionRepository.countByMedicalRecordIdAndStatus(
                medicalRecordId, ActionStatus.PENDING);
            Long cancelled = preventionActionRepository.countByMedicalRecordIdAndStatus(
                medicalRecordId, ActionStatus.CANCELLED);
            double rate = total > 0 ? Math.round((completed * 100.0 / total) * 10.0) / 10.0 : 0;

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalActions", total);
            stats.put("completedActions", completed);
            stats.put("pendingActions", pending);
            stats.put("cancelledActions", cancelled);
            stats.put("adherenceRate", rate);

            return ResponseEntity.ok(ApiResponse.success("Adherence stats retrieved", stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve adherence stats", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserOverview(@PathVariable Long userId) {
        try {
            Long totalRecords = medicalRecordRepository.countByUserId(userId);
            List<Object[]> latestRows = medicalRecordRepository.findLatestRiskScoreByUserId(userId);

            double currentRiskScore = 0;
            String currentRiskLevel = "LOW";
            Long latestRecordId = null;

            if (latestRows != null && !latestRows.isEmpty()) {
                Object[] latestRow = latestRows.get(0);
                latestRecordId = (Long) latestRow[0];
                currentRiskScore = latestRow[1] != null ? ((Number) latestRow[1]).doubleValue() : 0;
                currentRiskLevel = latestRow[2] != null ? latestRow[2].toString() : "LOW";
            }

            long totalRiskFactors = 0, activeRiskFactors = 0, totalActions = 0, completedActions = 0;
            if (latestRecordId != null) {
                totalRiskFactors = riskFactorRepository.countByMedicalRecordId(latestRecordId);
                activeRiskFactors = riskFactorRepository.countByMedicalRecordIdAndIsActive(latestRecordId, true);
                totalActions = preventionActionRepository.countByMedicalRecordId(latestRecordId);
                completedActions = preventionActionRepository.countByMedicalRecordIdAndStatus(latestRecordId, ActionStatus.COMPLETED);
            }

            double adherenceRate = totalActions > 0
                    ? Math.round((completedActions * 100.0 / totalActions) * 10.0) / 10.0 : 0;

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalRecords", totalRecords);
            stats.put("currentRiskScore", currentRiskScore);
            stats.put("currentRiskLevel", currentRiskLevel);
            stats.put("totalRiskFactors", totalRiskFactors);
            stats.put("activeRiskFactors", activeRiskFactors);
            stats.put("totalPreventionActions", totalActions);
            stats.put("completedActions", completedActions);
            stats.put("adherenceRate", adherenceRate);

            return ResponseEntity.ok(ApiResponse.success("Overview retrieved", stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve overview", e.getMessage()));
        }
    }

    @GetMapping("/global")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGlobalStats() {
        try {
            List<Object[]> riskDist = medicalRecordRepository.countByRiskLevel();
            Map<String, Long> riskDistMap = new LinkedHashMap<>();
            for (Object[] row : riskDist) riskDistMap.put(row[0].toString(), (Long) row[1]);

            List<Object[]> genderDist = medicalRecordRepository.countByGender();
            Map<String, Long> genderMap = new LinkedHashMap<>();
            for (Object[] row : genderDist) genderMap.put(row[0].toString(), (Long) row[1]);

            List<Object[]> familyHistDist = medicalRecordRepository.countByFamilyHistoryJPQL();
            Map<String, Long> familyHistMap = new LinkedHashMap<>();
            for (Object[] row : familyHistDist) familyHistMap.put(row[0].toString(), (Long) row[1]);

            Long totalRecords = medicalRecordRepository.countAllRecords();
            Double avgRisk = medicalRecordRepository.averageRiskScore();

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalRecords", totalRecords);
            stats.put("averageRiskScore", avgRisk != null ? Math.round(avgRisk * 10.0) / 10.0 : 0.0);
            stats.put("riskDistribution", riskDistMap);
            stats.put("genderDistribution", genderMap);
            stats.put("familyHistoryDistribution", familyHistMap);

            return ResponseEntity.ok(ApiResponse.success("Global stats retrieved", stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve global stats", e.getMessage()));
        }
    }
}
