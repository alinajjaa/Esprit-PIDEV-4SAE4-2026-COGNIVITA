package com.alzheimer.medicalrecords.controller;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;
import com.alzheimer.medicalrecords.service.*;
import com.alzheimer.medicalrecords.user.*;
import com.alzheimer.medicalrecords.dto.*;

import com.alzheimer.medicalrecords.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages medical appointments.
 *
 * Endpoints:
 *   GET  /api/appointments/medical-record/{recordId}          → list all for a record
 *   GET  /api/appointments/medical-record/{recordId}/upcoming → upcoming SCHEDULED only
 *   GET  /api/appointments/{id}                               → single
 *   POST /api/appointments                                     → create
 *   PUT  /api/appointments/{id}                               → update
 *   PATCH /api/appointments/{id}/complete                      → mark completed
 *   PATCH /api/appointments/{id}/cancel                        → cancel
 *   DELETE /api/appointments/{id}                              → delete
 *   GET  /api/appointments/medical-record/{recordId}/stats    → summary stats
 */
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentRepository   appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final TimelineService         timelineService;
    private final NotificationService     notificationService;

    public AppointmentController(AppointmentRepository appointmentRepository,
                                  MedicalRecordRepository medicalRecordRepository,
                                  TimelineService timelineService,
                                  @Qualifier("emailNotificationService") NotificationService notificationService) {
        this.appointmentRepository   = appointmentRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.timelineService         = timelineService;
        this.notificationService     = notificationService;
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @GetMapping("/medical-record/{recordId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Appointment>>> getByRecord(@PathVariable Long recordId) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Appointments retrieved",
                    appointmentRepository.findByMedicalRecordId(recordId)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve appointments", e.getMessage()));
        }
    }

    @GetMapping("/medical-record/{recordId}/upcoming")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Appointment>>> getUpcoming(@PathVariable Long recordId) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Upcoming appointments",
                    appointmentRepository.findByMedicalRecordIdAndStatus(recordId, AppointmentStatus.SCHEDULED)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Appointment>> getById(@PathVariable Long id) {
        return appointmentRepository.findById(id)
                .map(a -> ResponseEntity.ok(ApiResponse.success("Appointment found", a)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Not found", "No appointment with ID: " + id)));
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    @GetMapping("/medical-record/{recordId}/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats(@PathVariable Long recordId) {
        try {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("total",      appointmentRepository.countByMedicalRecordId(recordId));
            stats.put("scheduled",  appointmentRepository.countByMedicalRecordIdAndStatus(recordId, AppointmentStatus.SCHEDULED));
            stats.put("completed",  appointmentRepository.countByMedicalRecordIdAndStatus(recordId, AppointmentStatus.COMPLETED));
            stats.put("missed",     appointmentRepository.countByMedicalRecordIdAndStatus(recordId, AppointmentStatus.MISSED));
            stats.put("cancelled",  appointmentRepository.countByMedicalRecordIdAndStatus(recordId, AppointmentStatus.CANCELLED));
            long total     = (Long) stats.get("total");
            long completed = (Long) stats.get("completed");
            stats.put("attendanceRate", total > 0 ? Math.round(completed * 100.0 / total * 10) / 10.0 : 0.0);
            return ResponseEntity.ok(ApiResponse.success("Appointment stats", stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed", e.getMessage()));
        }
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<Appointment>> create(@RequestBody Map<String, Object> req) {
        try {
            if (req.get("medicalRecordId") == null)
                return ResponseEntity.badRequest().body(ApiResponse.error("Missing medicalRecordId", null));

            Long recordId = Long.valueOf(req.get("medicalRecordId").toString());
            Optional<MedicalRecord> opt = medicalRecordRepository.findByIdWithUser(recordId);
            if (opt.isEmpty())
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Record not found", "ID: " + recordId));

            Appointment appt = new Appointment();
            appt.setMedicalRecord(opt.get());
            applyRequest(req, appt);
            Appointment saved = appointmentRepository.save(appt);

            // Side effects in isolated try/catch — never affect the appointment save
            try { timelineService.logAppointmentScheduled(opt.get(), saved); }
            catch (Exception ex) { System.err.println("[APPT] Timeline log failed: " + ex.getMessage()); }
            try { notificationService.sendAppointmentConfirmation(opt.get(), saved); }
            catch (Exception ex) { System.err.println("[APPT] Email failed: " + ex.getMessage()); }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Appointment created", saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create appointment", e.getMessage()));
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Appointment>> update(@PathVariable Long id,
                                                            @RequestBody Map<String, Object> req) {
        try {
            return appointmentRepository.findById(id).map(appt -> {
                applyRequest(req, appt);
                return ResponseEntity.ok(ApiResponse.success("Appointment updated",
                        appointmentRepository.save(appt)));
            }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Not found", "ID: " + id)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update", e.getMessage()));
        }
    }

    // ── Status transitions ────────────────────────────────────────────────────

    @PatchMapping("/{id}/complete")
    @Transactional
    public ResponseEntity<ApiResponse<Appointment>> complete(@PathVariable Long id,
                                                              @RequestBody(required = false) Map<String, Object> req) {
        try {
            return appointmentRepository.findById(id).map(appt -> {
                appt.setStatus(AppointmentStatus.COMPLETED);
                appt.setCompletedAt(LocalDateTime.now());
                if (req != null && req.get("notes") != null)
                    appt.setNotes(req.get("notes").toString());
                Appointment saved = appointmentRepository.save(appt);
                try { timelineService.logAppointmentCompleted(appt.getMedicalRecord(), saved); }
                catch (Exception ex) { /* non-fatal */ }
                return ResponseEntity.ok(ApiResponse.success("Appointment marked completed", saved));
            }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Not found", "ID: " + id)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/cancel")
    @Transactional
    public ResponseEntity<ApiResponse<Appointment>> cancel(@PathVariable Long id) {
        try {
            return appointmentRepository.findById(id).map(appt -> {
                appt.setStatus(AppointmentStatus.CANCELLED);
                return ResponseEntity.ok(ApiResponse.success("Appointment cancelled",
                        appointmentRepository.save(appt)));
            }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Not found", "ID: " + id)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed", e.getMessage()));
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            if (!appointmentRepository.existsById(id))
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Not found", "ID: " + id));
            appointmentRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success("Appointment deleted", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed", e.getMessage()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void applyRequest(Map<String, Object> req, Appointment appt) {
        if (req.get("doctorName")   != null) appt.setDoctorName(req.get("doctorName").toString());
        if (req.get("specialty")    != null) appt.setSpecialty(req.get("specialty").toString());
        if (req.get("location")     != null) appt.setLocation(req.get("location").toString());
        if (req.get("notes")        != null) appt.setNotes(req.get("notes").toString());
        if (req.get("scheduledAt")  != null) {
            String raw = req.get("scheduledAt").toString().trim();
            // datetime-local sends "2026-03-02T14:30" (no seconds) — parse flexibly
            DateTimeFormatter flex = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm")
                .optionalStart().appendPattern(":ss").optionalEnd()
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .toFormatter();
            appt.setScheduledAt(LocalDateTime.parse(raw, flex));
        }
        if (req.get("appointmentType") != null) {
            try { appt.setAppointmentType(AppointmentType.valueOf(req.get("appointmentType").toString())); }
            catch (IllegalArgumentException ignored) {}
        }
        if (req.get("status") != null) {
            try { appt.setStatus(AppointmentStatus.valueOf(req.get("status").toString())); }
            catch (IllegalArgumentException ignored) {}
        }
    }
}
