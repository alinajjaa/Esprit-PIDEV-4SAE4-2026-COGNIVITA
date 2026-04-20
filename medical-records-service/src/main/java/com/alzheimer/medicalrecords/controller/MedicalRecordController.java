package com.alzheimer.medicalrecords.controller;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;
import com.alzheimer.medicalrecords.service.*;
import com.alzheimer.medicalrecords.user.*;
import com.alzheimer.medicalrecords.dto.*;

import com.alzheimer.medicalrecords.dto.ApiResponse;
import com.alzheimer.medicalrecords.user.User;
import com.alzheimer.medicalrecords.user.UserRepository;
import com.alzheimer.medicalrecords.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/medical-records")
public class MedicalRecordController {

    private final MedicalRecordRepository      medicalRecordRepository;
    private final UserRepository               userRepository;
    private final UserService                  userService;
    private final RiskScoreService             riskScoreService;
    private final RecommendationService        recommendationService;
    private final TimelineService              timelineService;
    private final RiskFactorRepository         riskFactorRepository;
    private final PreventionActionRepository   preventionActionRepository;
    private final TimelineRepository           timelineRepository;
    private final RiskScoreHistoryRepository   riskScoreHistoryRepository;
    private final NotificationService          notificationService;
    private final RiskTrendService             riskTrendService;

    public MedicalRecordController(
            MedicalRecordRepository medicalRecordRepository,
            UserRepository userRepository,
            UserService userService,
            RiskScoreService riskScoreService,
            RecommendationService recommendationService,
            TimelineService timelineService,
            RiskFactorRepository riskFactorRepository,
            PreventionActionRepository preventionActionRepository,
            TimelineRepository timelineRepository,
            RiskScoreHistoryRepository riskScoreHistoryRepository,
            @Qualifier("emailNotificationService") NotificationService notificationService,
            RiskTrendService riskTrendService) {
        this.medicalRecordRepository    = medicalRecordRepository;
        this.userRepository             = userRepository;
        this.userService                = userService;
        this.riskScoreService           = riskScoreService;
        this.recommendationService      = recommendationService;
        this.timelineService            = timelineService;
        this.riskFactorRepository       = riskFactorRepository;
        this.preventionActionRepository = preventionActionRepository;
        this.timelineRepository         = timelineRepository;
        this.riskScoreHistoryRepository = riskScoreHistoryRepository;
        this.notificationService        = notificationService;
        this.riskTrendService           = riskTrendService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllRecords(
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "10")   int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String familyHistory) {
        try {
            String col = switch (sortBy) {
                case "age"       -> "age";
                case "riskScore" -> "riskScore";
                default          -> "createdAt";
            };
            Sort sort = sortDirection.equalsIgnoreCase("ASC")
                    ? Sort.by(col).ascending() : Sort.by(col).descending();
            PageRequest pageable = PageRequest.of(page, size, sort);

            Page<Long> idPage;
            if (riskLevel != null && !riskLevel.isEmpty()) {
                idPage = medicalRecordRepository.findIdsByRiskLevel(
                        RiskLevel.valueOf(riskLevel.toUpperCase()), pageable);
            } else if (gender != null && !gender.isEmpty()) {
                idPage = medicalRecordRepository.findIdsByGender(Gender.valueOf(gender), pageable);
            } else if (familyHistory != null && !familyHistory.isEmpty()) {
                idPage = medicalRecordRepository.findIdsByFamilyHistory(
                        FamilyHistory.valueOf(familyHistory), pageable);
            } else {
                idPage = medicalRecordRepository.findAllIds(pageable);
            }

            List<MedicalRecordDTO> content = Collections.emptyList();
            if (!idPage.isEmpty()) {
                List<MedicalRecord> records =
                        medicalRecordRepository.findByIdsWithUser(idPage.getContent());
                content = records.stream().map(MedicalRecordDTO::new).collect(Collectors.toList());
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content",       content);
            result.put("totalElements", idPage.getTotalElements());
            result.put("totalPages",    idPage.getTotalPages());
            result.put("size",          idPage.getSize());
            result.put("number",        idPage.getNumber());

            return ResponseEntity.ok(ApiResponse.success("Medical records retrieved successfully", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve medical records", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<MedicalRecordDTO>> getRecordById(@PathVariable Long id) {
        try {
            return medicalRecordRepository.findByIdWithUser(id)
                    .map(r -> {
                        List<String> recs = recommendationService.generateRecommendations(
                                r, r.getRiskScore() != null ? r.getRiskScore() : 0);
                        return ResponseEntity.ok(
                                ApiResponse.success("Medical record found", new MedicalRecordDTO(r, recs)));
                    })
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error("Medical record not found", "No record with ID: " + id)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve medical record", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<MedicalRecordDTO>>> getRecordsByUser(
            @PathVariable Long userId) {
        try {
            List<MedicalRecordDTO> records = medicalRecordRepository.findByUserIdWithUser(userId)
                    .stream().map(MedicalRecordDTO::new).collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success("Records for user retrieved", records));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve records", e.getMessage()));
        }
    }

    @GetMapping("/check-user/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkUserHasRecord(
            @PathVariable Long userId) {
        try {
            List<MedicalRecord> records = medicalRecordRepository.findByUserIdWithUser(userId);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("hasRecord", !records.isEmpty());
            result.put("recordId",  records.isEmpty() ? null : records.get(0).getId());
            return ResponseEntity.ok(ApiResponse.success("User record check", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Check failed", e.getMessage()));
        }
    }

    @GetMapping("/{id}/dashboard")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPatientDashboard(
            @PathVariable Long id) {
        try {
            Optional<MedicalRecord> opt = medicalRecordRepository.findByIdWithUser(id);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Record not found", "No record with ID: " + id));
            }
            MedicalRecord record = opt.get();
            double riskScore = record.getRiskScore() != null ? record.getRiskScore() : 0;

            Long totalFactors  = riskFactorRepository.countByMedicalRecordId(id);
            Long activeFactors = riskFactorRepository.countByMedicalRecordIdAndIsActive(id, true);
            List<RiskFactor> activeList = riskFactorRepository.findByMedicalRecordIdAndIsActive(id, true);
            long criticalCount = activeList.stream().filter(rf -> rf.getSeverity() == Severity.CRITICAL).count();
            long highCount     = activeList.stream().filter(rf -> rf.getSeverity() == Severity.HIGH).count();

            Long totalActions     = preventionActionRepository.countByMedicalRecordId(id);
            Long completedActions = preventionActionRepository.countByMedicalRecordIdAndStatus(id, ActionStatus.COMPLETED);
            Long pendingActions   = preventionActionRepository.countByMedicalRecordIdAndStatus(id, ActionStatus.PENDING);
            double adherenceRate  = totalActions > 0
                    ? Math.round((completedActions * 100.0 / totalActions) * 10.0) / 10.0 : 0;
            long timelineCount = timelineRepository.findByMedicalRecordIdOrderByEventDateDesc(id).size();

            List<String> recommendations = recommendationService.generateRecommendations(record, riskScore);
            List<RecommendationService.PreventionActionSuggestion> suggestions =
                    recommendationService.suggestPreventionActions(record, riskScore);

            Map<String, Object> riskStats = new LinkedHashMap<>();
            riskStats.put("score",         riskScore);
            riskStats.put("level",         record.getRiskLevel() != null ? record.getRiskLevel().name() : "LOW");
            riskStats.put("totalFactors",  totalFactors);
            riskStats.put("activeFactors", activeFactors);
            riskStats.put("criticalCount", criticalCount);
            riskStats.put("highCount",     highCount);

            Map<String, Object> preventionStats = new LinkedHashMap<>();
            preventionStats.put("totalActions",     totalActions);
            preventionStats.put("completedActions", completedActions);
            preventionStats.put("pendingActions",   pendingActions);
            preventionStats.put("adherenceRate",    adherenceRate);

            Map<String, Object> dashboard = new LinkedHashMap<>();
            dashboard.put("record",          new MedicalRecordDTO(record, recommendations));
            dashboard.put("riskStats",        riskStats);
            dashboard.put("preventionStats",  preventionStats);
            dashboard.put("timelineCount",    timelineCount);
            dashboard.put("recommendations",  recommendations);
            dashboard.put("suggestedActions", suggestions);

            return ResponseEntity.ok(ApiResponse.success("Patient dashboard retrieved", dashboard));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve dashboard", e.getMessage()));
        }
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<MedicalRecordDTO>> createRecord(
            @RequestBody Map<String, Object> request) {
        try {
            if (request.get("userId") == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Missing userId", "userId is required"));
            }
            Long userId = Long.valueOf(request.get("userId").toString());
            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found", "No user with ID: " + userId));
            }
            MedicalRecord record = new MedicalRecord();
            record.setUser(userOpt.get());
            applyRequestToRecord(request, record);

            MedicalRecord saved = medicalRecordRepository.save(record);
            RiskLevel prevLevel = RiskLevel.LOW;
            riskScoreService.updateRiskScore(saved, "RECORD_CREATED");
            medicalRecordRepository.save(saved);
            notificationService.checkAndNotify(saved, prevLevel, "Medical record created");
            timelineService.logMedicalRecordUpdated(saved);

            List<String> recs = recommendationService.generateRecommendations(
                    saved, saved.getRiskScore() != null ? saved.getRiskScore() : 0);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Medical record created successfully",
                            new MedicalRecordDTO(saved, recs)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create medical record", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<MedicalRecordDTO>> updateRecord(
            @PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Optional<MedicalRecord> existingOpt = medicalRecordRepository.findByIdWithUser(id);
            if (existingOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Medical record not found", "No record with ID: " + id));
            }
            MedicalRecord record = existingOpt.get();
            RiskLevel prevLevel = record.getRiskLevel() != null ? record.getRiskLevel() : RiskLevel.LOW;
            applyRequestToRecord(request, record);

            MedicalRecord updated = medicalRecordRepository.save(record);
            riskScoreService.updateRiskScore(updated, "RECORD_UPDATED");
            medicalRecordRepository.save(updated);
            notificationService.checkAndNotify(updated, prevLevel, "Medical record updated");
            timelineService.logMedicalRecordUpdated(updated);

            List<String> recs = recommendationService.generateRecommendations(
                    updated, updated.getRiskScore() != null ? updated.getRiskScore() : 0);
            return ResponseEntity.ok(ApiResponse.success("Medical record updated successfully",
                    new MedicalRecordDTO(updated, recs)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update medical record", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/wellness-risk")
    @Transactional
    public ResponseEntity<ApiResponse<MedicalRecordDTO>> updateWellnessRisk(
            @PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Optional<MedicalRecord> opt = medicalRecordRepository.findByIdWithUser(id);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Medical record not found", "No record with ID: " + id));
            }
            MedicalRecord record = opt.get();
            if (request.get("wellnessRiskContribution") != null) {
                record.setWellnessRiskContribution(
                        Double.valueOf(request.get("wellnessRiskContribution").toString()));
            }
            MedicalRecord saved = medicalRecordRepository.save(record);
            riskScoreService.updateRiskScore(saved, "WELLNESS_PROFILE_UPDATED");
            medicalRecordRepository.save(saved);
            List<String> recs = recommendationService.generateRecommendations(
                    saved, saved.getRiskScore() != null ? saved.getRiskScore() : 0);
            return ResponseEntity.ok(ApiResponse.success("Wellness risk contribution updated",
                    new MedicalRecordDTO(saved, recs)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update wellness risk", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/hereditary-risk")
    @Transactional
    public ResponseEntity<ApiResponse<MedicalRecordDTO>> updateHereditaryRisk(
            @PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Optional<MedicalRecord> opt = medicalRecordRepository.findByIdWithUser(id);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Medical record not found", "No record with ID: " + id));
            }
            MedicalRecord record = opt.get();
            if (request.get("hereditaryRiskContribution") != null) {
                record.setHereditaryRiskContribution(
                        Double.valueOf(request.get("hereditaryRiskContribution").toString()));
            }
            MedicalRecord saved = medicalRecordRepository.save(record);
            riskScoreService.updateRiskScore(saved, "FAMILY_TREE_UPDATED");
            medicalRecordRepository.save(saved);
            List<String> recs = recommendationService.generateRecommendations(
                    saved, saved.getRiskScore() != null ? saved.getRiskScore() : 0);
            return ResponseEntity.ok(ApiResponse.success("Hereditary risk contribution updated",
                    new MedicalRecordDTO(saved, recs)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update hereditary risk", e.getMessage()));
        }
    }

    @GetMapping("/{id}/score-breakdown")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Double>>> getScoreBreakdown(@PathVariable Long id) {
        try {
            return medicalRecordRepository.findByIdWithUser(id)
                    .map(r -> ResponseEntity.ok(ApiResponse.success("Score breakdown",
                            riskScoreService.getScoreBreakdown(r))))
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error("Record not found", "No record with ID: " + id)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to compute score breakdown", e.getMessage()));
        }
    }

    @GetMapping("/{id}/risk-score-history")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<RiskScoreHistory>>> getRiskScoreHistory(
            @PathVariable Long id) {
        try {
            if (!medicalRecordRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Medical record not found", "No record with ID: " + id));
            }
            return ResponseEntity.ok(ApiResponse.success("Risk score history retrieved",
                    riskScoreService.getHistory(id)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve risk score history", e.getMessage()));
        }
    }


    // ── Session 3: Risk Trend & Projection ─────────────────────────────────────

    @GetMapping("/{id}/risk-trend")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRiskTrend(@PathVariable Long id) {
        try {
            if (!medicalRecordRepository.existsById(id))
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Record not found", "No record with ID: " + id));
            return ResponseEntity.ok(ApiResponse.success("Risk trend data", riskTrendService.getTrend(id)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to compute trend", e.getMessage()));
        }
    }

    @GetMapping("/{id}/risk-trend/chart")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRiskTrendChart(@PathVariable Long id) {
        try {
            if (!medicalRecordRepository.existsById(id))
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Record not found", "No record with ID: " + id));
            return ResponseEntity.ok(ApiResponse.success("Chart data", riskTrendService.getChartData(id)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed", e.getMessage()));
        }
    }

    @GetMapping("/population-comparison")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPopulationComparison(
            @RequestParam double score,
            @RequestParam(required = false) Integer age,
            @RequestParam(required = false) String gender) {
        try {
            // Score distribution benchmarks (based on published Alzheimer's epidemiology)
            double[] benchmarks = {10, 18, 25, 32, 40, 50, 60, 72, 85};
            long belowCount = java.util.Arrays.stream(benchmarks).filter(b -> b < score).count();
            double percentile = Math.round((belowCount / (double) benchmarks.length) * 100.0 * 10) / 10.0;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("yourScore",  Math.round(score * 10) / 10.0);
            result.put("percentile", percentile);
            result.put("interpretation", interpretPercentile(percentile));
            result.put("populationBenchmarks", Map.of(
                "averageScore",   28.0,
                "lowRiskAvg",     12.0,
                "mediumRiskAvg",  32.0,
                "highRiskAvg",    57.0,
                "criticalRiskAvg",78.0
            ));
            if (age != null) {
                result.put("ageGroupAverage", ageGroupAverage(age));
                result.put("ageComparison",   score > ageGroupAverage(age)
                        ? "Your score is above average for your age group."
                        : "Your score is below average for your age group — good.");
            }
            return ResponseEntity.ok(ApiResponse.success("Population comparison", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed", e.getMessage()));
        }
    }

    private String interpretPercentile(double percentile) {
        if (percentile < 25) return "Your risk score is in the lower 25% — lower risk than most.";
        if (percentile < 50) return "Your score is below the population median.";
        if (percentile < 75) return "Your score is above the population median.";
        return "Your score is in the top 25% highest — elevated compared to the general population.";
    }

    private double ageGroupAverage(int age) {
        if (age < 50) return 8;
        if (age < 60) return 15;
        if (age < 70) return 28;
        if (age < 80) return 42;
        return 58;
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteRecord(@PathVariable Long id) {
        try {
            if (!medicalRecordRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Medical record not found", "No record with ID: " + id));
            }
            riskScoreHistoryRepository.deleteByRecordId(id);
            medicalRecordRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success("Medical record deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete medical record", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        try {
            Long total     = medicalRecordRepository.countAllRecords();
            Double avgRisk = medicalRecordRepository.averageRiskScore();

            List<Object[]> riskDist = medicalRecordRepository.countByRiskLevel();
            Map<String, Long> riskDistMap = new LinkedHashMap<>();
            for (Object[] row : riskDist) riskDistMap.put(row[0].toString(), (Long) row[1]);

            List<Object[]> genderDist = medicalRecordRepository.countByGender();
            Map<String, Long> genderMap = new LinkedHashMap<>();
            for (Object[] row : genderDist) genderMap.put(row[0].toString(), (Long) row[1]);

            List<Object[]> familyHistDist = medicalRecordRepository.countByFamilyHistoryJPQL();
            Map<String, Long> familyHistMap = new LinkedHashMap<>();
            for (Object[] row : familyHistDist) familyHistMap.put(row[0].toString(), (Long) row[1]);

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalRecords",             total);
            stats.put("averageRiskScore",         avgRisk != null ? Math.round(avgRisk * 10.0) / 10.0 : 0.0);
            stats.put("riskDistribution",         riskDistMap);
            stats.put("genderDistribution",       genderMap);
            stats.put("familyHistoryDistribution",familyHistMap);
            stats.put("malePatients",             genderMap.getOrDefault("Male",   0L));
            stats.put("femalePatients",           genderMap.getOrDefault("Female", 0L));
            stats.put("withFamilyHistory",        familyHistMap.getOrDefault("Yes", 0L));

            return ResponseEntity.ok(ApiResponse.success("Statistics retrieved successfully", stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve statistics", e.getMessage()));
        }
    }

    // ORIGINAL helper + apoeStatus and diagnosisStage added at the end
    private void applyRequestToRecord(Map<String, Object> request, MedicalRecord record) {
        if (request.get("age")             != null) record.setAge(Integer.valueOf(request.get("age").toString()));
        if (request.get("gender")          != null) record.setGender(Gender.valueOf(request.get("gender").toString()));
        if (request.get("educationLevel")  != null) record.setEducationLevel(request.get("educationLevel").toString());
        if (request.get("familyHistory")   != null) record.setFamilyHistory(FamilyHistory.valueOf(request.get("familyHistory").toString()));
        if (request.get("riskFactors")     != null) record.setRiskFactors(request.get("riskFactors").toString());
        if (request.get("currentSymptoms") != null) record.setCurrentSymptoms(request.get("currentSymptoms").toString());
        if (request.get("diagnosisNotes")  != null) record.setDiagnosisNotes(request.get("diagnosisNotes").toString());
        // NEW fields — safely ignored if not sent by frontend
        if (request.get("apoeStatus") != null) {
            try { record.setApoeStatus(APOEStatus.valueOf(request.get("apoeStatus").toString())); }
            catch (IllegalArgumentException ignored) {}
        }
        if (request.get("diagnosisStage") != null) {
            try { record.setDiagnosisStage(DiagnosisStage.valueOf(request.get("diagnosisStage").toString())); }
            catch (IllegalArgumentException ignored) {}
        }
    }
}
