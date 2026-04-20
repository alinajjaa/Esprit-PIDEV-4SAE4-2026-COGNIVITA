package com.alzheimer.medicalrecords.controller;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;
import com.alzheimer.medicalrecords.service.*;
import com.alzheimer.medicalrecords.user.*;
import com.alzheimer.medicalrecords.dto.*;

import com.alzheimer.medicalrecords.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/medications")
public class MedicationController {

    private static final Logger log = LoggerFactory.getLogger(MedicationController.class);

    private final MedicationRepository medicationRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final TimelineService timelineService;
    private final MedicationInteractionService interactionService;
    private final RestTemplate restTemplate;
    private final UserService userService;

    @Value("${medication-adherence.base-url:http://localhost:8086}")
    private String adherenceServiceUrl;

    // Drugs known to have cognitive / dementia risk implications
    private static final Map<String, String> DRUG_RISK_MAP = Map.ofEntries(
        Map.entry("donepezil",       "PROTECTIVE"),
        Map.entry("rivastigmine",    "PROTECTIVE"),
        Map.entry("galantamine",     "PROTECTIVE"),
        Map.entry("memantine",       "PROTECTIVE"),
        Map.entry("aricept",         "PROTECTIVE"),
        Map.entry("exelon",          "PROTECTIVE"),
        Map.entry("razadyne",        "PROTECTIVE"),
        Map.entry("namenda",         "PROTECTIVE"),
        Map.entry("diphenhydramine", "RISK"),
        Map.entry("benadryl",        "RISK"),
        Map.entry("diazepam",        "RISK"),
        Map.entry("lorazepam",       "RISK"),
        Map.entry("alprazolam",      "RISK"),
        Map.entry("zolpidem",        "RISK"),
        Map.entry("amitriptyline",   "RISK"),
        Map.entry("oxybutynin",      "RISK"),
        Map.entry("lithium",         "RISK"),
        Map.entry("haloperidol",     "RISK"),
        Map.entry("quetiapine",      "RISK"),
        Map.entry("metformin",       "PROTECTIVE"),
        Map.entry("statins",         "PROTECTIVE"),
        Map.entry("atorvastatin",    "PROTECTIVE"),
        Map.entry("simvastatin",     "PROTECTIVE")
    );

    public MedicationController(MedicationRepository medicationRepository,
                                 MedicalRecordRepository medicalRecordRepository,
                                 TimelineService timelineService,
                                 MedicationInteractionService interactionService,
                                 RestTemplate restTemplate,
                                 UserService userService) {
        this.medicationRepository    = medicationRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.timelineService         = timelineService;
        this.interactionService      = interactionService;
        this.restTemplate            = restTemplate;
        this.userService             = userService;
    }

    /** GET /api/medications/medical-record/{recordId}/interactions */
    @GetMapping("/medical-record/{recordId}/interactions")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInteractions(@PathVariable Long recordId) {
        try {
            List<Medication> meds = medicationRepository.findByMedicalRecordId(recordId);
            List<MedicationInteractionService.InteractionAlert> alerts = interactionService.analyze(meds);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("alerts",  alerts);
            result.put("summary", interactionService.summary(alerts));
            return ResponseEntity.ok(ApiResponse.success("Interaction analysis complete", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed", e.getMessage()));
        }
    }

    @GetMapping("/medical-record/{recordId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Medication>>> getByRecord(@PathVariable Long recordId) {
        try {
            List<Medication> meds = medicationRepository.findByMedicalRecordId(recordId);
            return ResponseEntity.ok(ApiResponse.success("Medications retrieved", meds));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve medications", e.getMessage()));
        }
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<Medication>> create(@RequestBody Map<String, Object> req) {
        try {
            // Validate required field
            if (req.get("name") == null || req.get("name").toString().isBlank())
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Validation failed", "Medication name is required"));

            Long recordId = Long.valueOf(req.get("medicalRecordId").toString());
            Optional<MedicalRecord> opt = medicalRecordRepository.findByIdWithUser(recordId);
            if (opt.isEmpty())
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Record not found", "No record with ID: " + recordId));

            Medication med = new Medication();
            med.setMedicalRecord(opt.get());
            applyRequest(req, med);
            med.setRiskFlag(detectRiskFlag(med.getName()));
            Medication saved = medicationRepository.save(med);
            timelineService.logMedicalRecordUpdated(opt.get());

            // Sync with medication-adherence-service
            syncScheduleDoseToAdherence(saved, opt.get());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Medication added", saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to add medication", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Medication>> update(@PathVariable Long id,
                                                           @RequestBody Map<String, Object> req) {
        try {
            return medicationRepository.findById(id).map(med -> {
                MedicalRecord record = medicalRecordRepository
                        .findByIdWithUser(med.getMedicalRecord().getId())
                        .orElse(med.getMedicalRecord()); // fetch with user eagerly loaded for sync
                applyRequest(req, med);
                med.setRiskFlag(detectRiskFlag(med.getName()));
                Medication saved = medicationRepository.save(med);

                // Re-sync with adherence-service on update (name/dosage/frequency may have changed)
                syncScheduleDoseToAdherence(saved, record);

                return ResponseEntity.ok(ApiResponse.success("Medication updated", saved));
            }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Medication not found", "ID: " + id)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update medication", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            if (!medicationRepository.existsById(id))
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Medication not found", "ID: " + id));
            medicationRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success("Medication deleted", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete medication", e.getMessage()));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void applyRequest(Map<String, Object> req, Medication med) {
        if (req.get("name")        != null) med.setName(req.get("name").toString());
        if (req.get("dosage")      != null) med.setDosage(req.get("dosage").toString());
        if (req.get("frequency")   != null) med.setFrequency(req.get("frequency").toString());
        if (req.get("prescribedBy")!= null) med.setPrescribedBy(req.get("prescribedBy").toString());
        if (req.get("notes")       != null) med.setNotes(req.get("notes").toString());
        if (req.get("isActive")    != null) med.setIsActive(Boolean.valueOf(req.get("isActive").toString()));
        if (req.get("startDate")   != null) med.setStartDate(LocalDate.parse(req.get("startDate").toString()));
        if (req.get("endDate")     != null) med.setEndDate(LocalDate.parse(req.get("endDate").toString()));
    }

    private String detectRiskFlag(String name) {
        if (name == null) return "NEUTRAL";
        String lower = name.toLowerCase();
        for (Map.Entry<String, String> entry : DRUG_RISK_MAP.entrySet()) {
            if (lower.contains(entry.getKey())) return entry.getValue();
        }
        return "NEUTRAL";
    }

    /**
     * Notifies medication-adherence-service about a new or updated medication
     * so it can schedule dose tracking logs for today.
     * Failures are logged but do NOT fail the medication save — adherence sync
     * is best-effort and the scheduler will catch up on missed doses.
     *
     * FIX: scheduledTime is now a LocalDateTime object (not a .toString() String)
     * so the configured JavaTimeModule serialises it as ISO-8601 and the
     * adherence service's ScheduleDoseRequest record can deserialise it correctly.
     *
     * FIX: always schedule for today — using startDate (which may be in the past)
     * caused the adherence service's duplicate-check to skip the entry silently.
     */
    private void syncScheduleDoseToAdherence(Medication med, MedicalRecord record) {
        try {
            if (!Boolean.TRUE.equals(med.getIsActive())) return;

            User user = record.getUser();
            // LEFT JOIN FETCH returns null when the user_id FK has no matching row in
            // the local users table (possible with NO_CONSTRAINT DDL mode or if the
            // user was stored under a different local ID). Fall back to UserService
            // which will fetch & cache the user from the remote user-service.
            if (user == null && record.getUserId() != null) {
                user = userService.findById(record.getUserId()).orElse(null);
            }
            if (user == null) {
                log.warn("[MedicationController] Cannot sync medication id={} — no user found for medical record id={}",
                        med.getId(), record.getId());
                return;
            }

            // Always schedule for today at 9 am so the adherence service creates
            // a PENDING log for the current day, regardless of the medication's startDate.
            LocalDateTime scheduledTime = LocalDate.now().atTime(LocalTime.of(9, 0));

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("patientUserId",   user.getId());
            payload.put("medicationId",    med.getId());
            payload.put("medicationName",  med.getName());
            payload.put("dosage",          med.getDosage() != null ? med.getDosage() : "");
            payload.put("frequency",       med.getFrequency() != null ? med.getFrequency() : "");
            // Pass the LocalDateTime object — not .toString() — so Jackson serialises
            // it via JavaTimeModule as "2026-04-15T09:00:00" rather than a raw string
            // that Spring cannot bind to a LocalDateTime record component.
            payload.put("scheduledTime",   scheduledTime);
            payload.put("patientName",     user.getFirstName() + " " + user.getLastName());
            payload.put("patientEmail",    user.getEmail());
            payload.put("patientPhone",    user.getPhone() != null ? user.getPhone() : "");
            payload.put("caregiverEmail",  record.getCaregiverEmail() != null ? record.getCaregiverEmail() : "");
            payload.put("caregiverPhone",  record.getCaregiverPhone() != null ? record.getCaregiverPhone() : "");
            payload.put("doctorEmail",     record.getDoctorEmail()    != null ? record.getDoctorEmail()    : "");
            payload.put("doctorPhone",     record.getDoctorPhone()    != null ? record.getDoctorPhone()    : "");

            restTemplate.postForEntity(
                adherenceServiceUrl + "/api/adherence/schedule",
                payload,
                Map.class
            );
            log.info("[MedicationController] Synced medication {} (id={}) to adherence-service for patient {}",
                    med.getName(), med.getId(), user.getId());
        } catch (Exception e) {
            log.warn("[MedicationController] Could not sync medication {} to adherence-service: {}",
                    med.getId(), e.getMessage());
        }
    }
}
