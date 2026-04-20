package com.alzheimer.notification.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Tracks an ongoing escalation chain for a patient.
 * When a missed-dose event fires, an EscalationRule row is created.
 * The scheduler advances through levels: patient → caregiver → doctor.
 */
@Entity
@Table(name = "escalation_rules", indexes = {
    @Index(name = "idx_esc_patient_id", columnList = "patient_user_id"),
    @Index(name = "idx_esc_active",     columnList = "active"),
    @Index(name = "idx_esc_type",       columnList = "escalation_type")
})
public class EscalationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_user_id", nullable = false)
    private Long patientUserId;

    @Column(name = "caregiver_email", length = 200)
    private String caregiverEmail;

    @Column(name = "caregiver_phone", length = 30)
    private String caregiverPhone;

    @Column(name = "doctor_email", length = 200)
    private String doctorEmail;

    @Column(name = "doctor_phone", length = 30)
    private String doctorPhone;

    @Column(name = "escalation_type", length = 50)
    @Enumerated(EnumType.STRING)
    private EscalationType escalationType;

    @Column(name = "reference_id")
    private Long referenceId; // medicationId, appointmentId, etc.

    @Column(name = "context_message", columnDefinition = "TEXT")
    private String contextMessage;

    /** Current level: 0=not yet escalated, 1=caregiver notified, 2=doctor notified */
    @Column(name = "current_level")
    private Integer currentLevel = 0;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "triggered_at", nullable = false, updatable = false)
    private LocalDateTime triggeredAt = LocalDateTime.now();

    @Column(name = "last_escalated_at")
    private LocalDateTime lastEscalatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // ── Getters / Setters ────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPatientUserId() { return patientUserId; }
    public void setPatientUserId(Long patientUserId) { this.patientUserId = patientUserId; }
    public String getCaregiverEmail() { return caregiverEmail; }
    public void setCaregiverEmail(String caregiverEmail) { this.caregiverEmail = caregiverEmail; }
    public String getCaregiverPhone() { return caregiverPhone; }
    public void setCaregiverPhone(String caregiverPhone) { this.caregiverPhone = caregiverPhone; }
    public String getDoctorEmail() { return doctorEmail; }
    public void setDoctorEmail(String doctorEmail) { this.doctorEmail = doctorEmail; }
    public String getDoctorPhone() { return doctorPhone; }
    public void setDoctorPhone(String doctorPhone) { this.doctorPhone = doctorPhone; }
    public EscalationType getEscalationType() { return escalationType; }
    public void setEscalationType(EscalationType escalationType) { this.escalationType = escalationType; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public String getContextMessage() { return contextMessage; }
    public void setContextMessage(String contextMessage) { this.contextMessage = contextMessage; }
    public Integer getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(Integer currentLevel) { this.currentLevel = currentLevel; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getTriggeredAt() { return triggeredAt; }
    public void setTriggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; }
    public LocalDateTime getLastEscalatedAt() { return lastEscalatedAt; }
    public void setLastEscalatedAt(LocalDateTime lastEscalatedAt) { this.lastEscalatedAt = lastEscalatedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
