package com.alzheimer.medicalrecords.controller;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;
import com.alzheimer.medicalrecords.service.*;
import com.alzheimer.medicalrecords.user.*;
import com.alzheimer.medicalrecords.dto.*;

import com.alzheimer.medicalrecords.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/medications")
public class MedicationController {

    private final MedicationRepository medicationRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final TimelineService timelineService;
    private final MedicationInteractionService interactionService;

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
                                 MedicationInteractionService interactionService) {
        this.medicationRepository   = medicationRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.timelineService        = timelineService;
        this.interactionService     = interactionService;
    }

    /** GET /api/medications/medical-record/{recordId}/interactions
     *  Returns interaction alerts and a summary for all active medications. */
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
                applyRequest(req, med);
                med.setRiskFlag(detectRiskFlag(med.getName()));
                Medication saved = medicationRepository.save(med);
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
}
