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

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/care-notes")
public class CareNoteController {

    private final CareNoteRepository careNoteRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final TimelineService timelineService;

    public CareNoteController(CareNoteRepository careNoteRepository,
                               MedicalRecordRepository medicalRecordRepository,
                               TimelineService timelineService) {
        this.careNoteRepository      = careNoteRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.timelineService         = timelineService;
    }

    @GetMapping("/medical-record/{recordId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<CareNote>>> getByRecord(@PathVariable Long recordId) {
        try {
            List<CareNote> notes = careNoteRepository
                    .findByMedicalRecordIdOrderByIsPinnedDescCreatedAtDesc(recordId);
            return ResponseEntity.ok(ApiResponse.success("Care notes retrieved", notes));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve care notes", e.getMessage()));
        }
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<CareNote>> create(@RequestBody Map<String, Object> req) {
        try {
            // Validate required fields — authorName and content are NOT NULL in DB
            if (req.get("authorName") == null || req.get("authorName").toString().isBlank())
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Validation failed", "authorName is required"));
            if (req.get("content") == null || req.get("content").toString().isBlank())
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Validation failed", "content is required"));

            // FIX: guard before Long.valueOf — a missing medicalRecordId previously
            // threw NullPointerException inside Long.valueOf(null.toString()),
            // returning a 500 instead of a clean 400 to the dashboard.
            if (req.get("medicalRecordId") == null)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Validation failed", "medicalRecordId is required"));

            Long recordId;
            try {
                recordId = Long.valueOf(req.get("medicalRecordId").toString());
            } catch (NumberFormatException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Validation failed", "medicalRecordId must be a valid number"));
            }
            Optional<MedicalRecord> opt = medicalRecordRepository.findById(recordId);
            if (opt.isEmpty())
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Record not found", "ID: " + recordId));

            CareNote note = new CareNote();
            note.setMedicalRecord(opt.get());
            applyRequest(req, note);
            CareNote saved = careNoteRepository.save(note);
            timelineService.logMedicalRecordUpdated(opt.get());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Care note added", saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to add care note", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<CareNote>> update(@PathVariable Long id,
                                                         @RequestBody Map<String, Object> req) {
        try {
            // Validate required fields on update too
            if (req.containsKey("authorName") &&
                    (req.get("authorName") == null || req.get("authorName").toString().isBlank()))
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Validation failed", "authorName cannot be blank"));
            if (req.containsKey("content") &&
                    (req.get("content") == null || req.get("content").toString().isBlank()))
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Validation failed", "content cannot be blank"));

            return careNoteRepository.findById(id).map(note -> {
                applyRequest(req, note);
                CareNote saved = careNoteRepository.save(note);
                return ResponseEntity.ok(ApiResponse.success("Care note updated", saved));
            }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Note not found", "ID: " + id)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update care note", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/pin")
    @Transactional
    public ResponseEntity<ApiResponse<CareNote>> togglePin(@PathVariable Long id) {
        try {
            return careNoteRepository.findById(id).map(note -> {
                note.setIsPinned(!Boolean.TRUE.equals(note.getIsPinned()));
                CareNote saved = careNoteRepository.save(note);
                return ResponseEntity.ok(ApiResponse.success("Pin toggled", saved));
            }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Note not found", "ID: " + id)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to pin note", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            if (!careNoteRepository.existsById(id))
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Note not found", "ID: " + id));
            careNoteRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success("Care note deleted", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete care note", e.getMessage()));
        }
    }

    private void applyRequest(Map<String, Object> req, CareNote note) {
        if (req.get("authorName") != null) note.setAuthorName(req.get("authorName").toString());
        if (req.get("authorRole") != null) note.setAuthorRole(req.get("authorRole").toString());
        if (req.get("content")    != null) note.setContent(req.get("content").toString());
        if (req.get("noteType")   != null) note.setNoteType(req.get("noteType").toString());
        if (req.get("isPinned")   != null) note.setIsPinned(Boolean.valueOf(req.get("isPinned").toString()));
    }
}
