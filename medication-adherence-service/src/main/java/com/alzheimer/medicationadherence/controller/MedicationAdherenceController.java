package com.alzheimer.medicationadherence.controller;

import com.alzheimer.medicationadherence.dto.AdherenceDTOs.*;
import com.alzheimer.medicationadherence.entity.AdherenceScore;
import com.alzheimer.medicationadherence.entity.MedicationLog;
import com.alzheimer.medicationadherence.service.AdherenceService;
import com.alzheimer.medicationadherence.service.AdherenceService.DrugInteractionReport;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/adherence")
public class MedicationAdherenceController {

    private final AdherenceService adherenceService;

    public MedicationAdherenceController(AdherenceService adherenceService) {
        this.adherenceService = adherenceService;
    }

    // ── Dose scheduling (called by medical-records-service or admin) ──────────

    @PostMapping("/schedule")
    public ResponseEntity<MedicationLog> scheduleDose(@RequestBody ScheduleDoseRequest req) {
        MedicationLog log = adherenceService.scheduleDose(
            req.patientUserId(), req.medicationId(), req.medicationName(),
            req.dosage(), req.frequency(), req.scheduledTime(),
            req.patientName(), req.patientEmail(), req.patientPhone(),
            req.caregiverEmail(), req.caregiverPhone(),
            req.doctorEmail(), req.doctorPhone()
        );
        return ResponseEntity.ok(log);
    }

    // ── Patient check-in ──────────────────────────────────────────────────────

    /**
     * POST /api/adherence/check-in
     * Patient confirms they have taken a dose.
     */
    @PostMapping("/check-in")
    public ResponseEntity<MedicationLog> checkIn(@RequestBody CheckInRequest req) {
        MedicationLog log = adherenceService.confirmDoseTaken(
            req.patientUserId(), req.medicationId(), req.date(), req.notes()
        );
        return ResponseEntity.ok(log);
    }

    /**
     * POST /api/adherence/skip
     * Patient skips a dose (with optional reason).
     */
    @PostMapping("/skip")
    public ResponseEntity<MedicationLog> skipDose(@RequestBody SkipDoseRequest req) {
        MedicationLog log = adherenceService.skipDose(
            req.patientUserId(), req.medicationId(), req.date(), req.reason()
        );
        return ResponseEntity.ok(log);
    }

    // ── Today's doses ─────────────────────────────────────────────────────────

    @GetMapping("/today/{patientUserId}")
    public ResponseEntity<List<MedicationLog>> getTodaysDoses(@PathVariable Long patientUserId) {
        return ResponseEntity.ok(adherenceService.getTodaysLogs(patientUserId));
    }

    @GetMapping("/{patientUserId}/date/{date}")
    public ResponseEntity<List<MedicationLog>> getDosesForDate(
            @PathVariable Long patientUserId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(adherenceService.getLogsForDate(patientUserId, date));
    }

    // ── Adherence score ───────────────────────────────────────────────────────

    @GetMapping("/{patientUserId}/score")
    public ResponseEntity<AdherenceSummaryResponse> getAdherenceScore(@PathVariable Long patientUserId) {
        AdherenceScore score = adherenceService.getAdherenceScore(patientUserId)
            .orElseGet(() -> {
                AdherenceScore empty = new AdherenceScore();
                empty.setPatientUserId(patientUserId);
                return empty;
            });
        String level = score.getWeeklyScore() >= 90 ? "EXCELLENT"
            : score.getWeeklyScore() >= 70 ? "GOOD"
            : score.getWeeklyScore() >= 50 ? "FAIR" : "POOR";
        return ResponseEntity.ok(new AdherenceSummaryResponse(
            patientUserId,
            score.getWeeklyScore() != null ? score.getWeeklyScore() : 100.0,
            score.getMonthlyScore() != null ? score.getMonthlyScore() : 100.0,
            score.getStreakDays() != null ? score.getStreakDays() : 0,
            score.getTotalDosesScheduled() != null ? score.getTotalDosesScheduled() : 0,
            score.getTotalDosesTaken() != null ? score.getTotalDosesTaken() : 0,
            score.getTotalDosesMissed() != null ? score.getTotalDosesMissed() : 0,
            score.getLastCalculated(),
            level
        ));
    }

    /** Force a score recalculation and publish to medical-records-service */
    @PostMapping("/{patientUserId}/recalculate")
    public ResponseEntity<AdherenceScore> recalculate(@PathVariable Long patientUserId) {
        return ResponseEntity.ok(adherenceService.recalculateAndPublish(patientUserId));
    }

    // ── History ───────────────────────────────────────────────────────────────

    @GetMapping("/{patientUserId}/history")
    public ResponseEntity<List<MedicationLog>> getHistory(@PathVariable Long patientUserId) {
        return ResponseEntity.ok(adherenceService.getHistory(patientUserId));
    }

    // ── Drug interaction checking ─────────────────────────────────────────────

    /**
     * POST /api/adherence/drug-interactions/check
     * Surfaces the MedicationInteractionService logic.
     * Body: { "medicationNames": ["donepezil", "amitriptyline", "lorazepam"] }
     */
    @PostMapping("/drug-interactions/check")
    public ResponseEntity<DrugInteractionReport> checkInteractions(
            @RequestBody DrugInteractionCheckRequest req) {
        return ResponseEntity.ok(adherenceService.checkInteractions(req.medicationNames()));
    }

    /** Convenience GET for quick checks */
    @GetMapping("/drug-interactions/check")
    public ResponseEntity<DrugInteractionReport> checkInteractionsGet(
            @RequestParam List<String> medications) {
        return ResponseEntity.ok(adherenceService.checkInteractions(medications));
    }

    // ── Health ────────────────────────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "medication-adherence-service"));
    }
}
