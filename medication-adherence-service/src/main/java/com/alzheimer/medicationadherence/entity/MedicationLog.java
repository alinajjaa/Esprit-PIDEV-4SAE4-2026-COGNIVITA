package com.alzheimer.medicationadherence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "medication_logs", indexes = {
    @Index(name = "idx_log_patient",    columnList = "patient_user_id"),
    @Index(name = "idx_log_medication", columnList = "medication_id"),
    @Index(name = "idx_log_status",     columnList = "status"),
    @Index(name = "idx_log_scheduled",  columnList = "scheduled_time"),
    @Index(name = "idx_log_date",       columnList = "scheduled_date")
})
public class MedicationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_user_id", nullable = false)
    private Long patientUserId;

    /** References Medication.id in medical-records-service */
    @Column(name = "medication_id", nullable = false)
    private Long medicationId;

    @Column(name = "medication_name", nullable = false, length = 200)
    private String medicationName;

    @Column(name = "dosage", length = 100)
    private String dosage;

    @Column(name = "frequency", length = 100)
    private String frequency;

    /** When this dose was supposed to be taken */
    @Column(name = "scheduled_time", nullable = false)
    private LocalDateTime scheduledTime;

    @Column(name = "scheduled_date", nullable = false)
    private java.time.LocalDate scheduledDate;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DoseStatus status = DoseStatus.PENDING;

    /** When the patient confirmed intake */
    @Column(name = "taken_at")
    private LocalDateTime takenAt;

    @Column(name = "patient_notes", columnDefinition = "TEXT")
    private String patientNotes;

    /** Caregiver / doctor contact info (cached from medical-records at log creation time) */
    @Column(name = "caregiver_email", length = 200)
    private String caregiverEmail;

    @Column(name = "caregiver_phone", length = 30)
    private String caregiverPhone;

    @Column(name = "doctor_email", length = 200)
    private String doctorEmail;

    @Column(name = "doctor_phone", length = 30)
    private String doctorPhone;

    @Column(name = "patient_email", length = 200)
    private String patientEmail;

    @Column(name = "patient_phone", length = 30)
    private String patientPhone;

    @Column(name = "patient_name", length = 200)
    private String patientName;

    /** True if a missed-dose event has already been published for this log */
    @Column(name = "alert_sent")
    private Boolean alertSent = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }

    // ── Getters / Setters ────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPatientUserId() { return patientUserId; }
    public void setPatientUserId(Long patientUserId) { this.patientUserId = patientUserId; }
    public Long getMedicationId() { return medicationId; }
    public void setMedicationId(Long medicationId) { this.medicationId = medicationId; }
    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }
    public java.time.LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(java.time.LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }
    public DoseStatus getStatus() { return status; }
    public void setStatus(DoseStatus status) { this.status = status; }
    public LocalDateTime getTakenAt() { return takenAt; }
    public void setTakenAt(LocalDateTime takenAt) { this.takenAt = takenAt; }
    public String getPatientNotes() { return patientNotes; }
    public void setPatientNotes(String patientNotes) { this.patientNotes = patientNotes; }
    public String getCaregiverEmail() { return caregiverEmail; }
    public void setCaregiverEmail(String caregiverEmail) { this.caregiverEmail = caregiverEmail; }
    public String getCaregiverPhone() { return caregiverPhone; }
    public void setCaregiverPhone(String caregiverPhone) { this.caregiverPhone = caregiverPhone; }
    public String getDoctorEmail() { return doctorEmail; }
    public void setDoctorEmail(String doctorEmail) { this.doctorEmail = doctorEmail; }
    public String getDoctorPhone() { return doctorPhone; }
    public void setDoctorPhone(String doctorPhone) { this.doctorPhone = doctorPhone; }
    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }
    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public Boolean getAlertSent() { return alertSent; }
    public void setAlertSent(Boolean alertSent) { this.alertSent = alertSent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
