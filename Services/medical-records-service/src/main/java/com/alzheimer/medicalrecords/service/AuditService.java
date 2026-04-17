package com.alzheimer.medicalrecords.service;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper that persists AuditLog entries.
 * All writes are fire-and-forget (any exception is logged but never propagates
 * so it can never break the main operation flow).
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String entityType, Long entityId, String action, String performedBy) {
        log(entityType, entityId, action, performedBy, null);
    }

    public void log(String entityType, Long entityId, String action,
                    String performedBy, String changesJson) {
        try {
            auditLogRepository.save(new AuditLog(entityType, entityId, action, performedBy, changesJson));
        } catch (Exception e) {
            log.error("[AUDIT] Failed to write audit log — entity={} id={} action={}: {}",
                    entityType, entityId, action, e.getMessage());
        }
    }

    public void logRecordCreated(Long recordId, Long userId) {
        log("MedicalRecord", recordId, "CREATE", "USER:" + userId,
                "{\"event\":\"medical_record_created\",\"userId\":" + userId + "}");
    }

    public void logRecordUpdated(Long recordId, Long userId) {
        log("MedicalRecord", recordId, "UPDATE", "USER:" + userId,
                "{\"event\":\"medical_record_updated\",\"userId\":" + userId + "}");
    }

    public void logRecordDeleted(Long recordId, Long userId) {
        log("MedicalRecord", recordId, "DELETE", "USER:" + userId,
                "{\"event\":\"medical_record_deleted\",\"userId\":" + userId + "}");
    }

    public void logPdfExported(Long recordId, Long userId) {
        log("MedicalRecord", recordId, "EXPORT_PDF", "USER:" + userId,
                "{\"event\":\"pdf_exported\",\"userId\":" + userId + "}");
    }

    public void logAppointmentCreated(Long appointmentId, Long recordId) {
        log("Appointment", appointmentId, "CREATE", "SYSTEM",
                "{\"medicalRecordId\":" + recordId + "}");
    }

    public void logRiskAlertSent(Long recordId, String level, double score) {
        log("MedicalRecord", recordId, "RISK_ALERT",  "SYSTEM",
                "{\"riskLevel\":\"" + level + "\",\"score\":" + score + "}");
    }
}
