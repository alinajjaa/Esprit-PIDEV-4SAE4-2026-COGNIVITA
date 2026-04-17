package com.alzheimer.medicalrecords.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments", indexes = {
        @Index(name = "idx_appt_record_id",    columnList = "medical_record_id"),
        @Index(name = "idx_appt_scheduled_at", columnList = "scheduled_at"),
        @Index(name = "idx_appt_status",       columnList = "status")
})
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id", nullable = false, insertable = false, updatable = false)
    private MedicalRecord medicalRecord;

    @Column(name = "medical_record_id", nullable = false)
    private Long medicalRecordId;

    @Column(name = "doctor_name", length = 150)
    private String doctorName;

    @Column(name = "specialty", length = 100)
    private String specialty;                      // e.g. Neurologist, GP, Memory Clinic

    @Column(name = "appointment_type", length = 50)
    @Enumerated(EnumType.STRING)
    private AppointmentType appointmentType = AppointmentType.GENERAL;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "status", length = 30)
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** Set when appointment is completed */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Reminder sent flag — set by the scheduler so it doesn't re-send */
    @Column(name = "reminder_sent")
    private Boolean reminderSent = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }

    // ── Getters / Setters ──
    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }
    public MedicalRecord getMedicalRecord()     { return medicalRecord; }
    public void setMedicalRecord(MedicalRecord r) { this.medicalRecord = r; if (r != null) this.medicalRecordId = r.getId(); }
    public Long getMedicalRecordId() { return medicalRecordId; }
    public void setMedicalRecordId(Long id) { this.medicalRecordId = id; }
    public String getDoctorName()               { return doctorName; }
    public void setDoctorName(String v)         { this.doctorName = v; }
    public String getSpecialty()                { return specialty; }
    public void setSpecialty(String v)          { this.specialty = v; }
    public AppointmentType getAppointmentType() { return appointmentType; }
    public void setAppointmentType(AppointmentType v) { this.appointmentType = v; }
    public LocalDateTime getScheduledAt()       { return scheduledAt; }
    public void setScheduledAt(LocalDateTime v) { this.scheduledAt = v; }
    public AppointmentStatus getStatus()        { return status; }
    public void setStatus(AppointmentStatus v)  { this.status = v; }
    public String getLocation()                 { return location; }
    public void setLocation(String v)           { this.location = v; }
    public String getNotes()                    { return notes; }
    public void setNotes(String v)              { this.notes = v; }
    public LocalDateTime getCompletedAt()       { return completedAt; }
    public void setCompletedAt(LocalDateTime v) { this.completedAt = v; }
    public Boolean getReminderSent()            { return reminderSent; }
    public void setReminderSent(Boolean v)      { this.reminderSent = v; }
    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime v)   { this.createdAt = v; }
    public LocalDateTime getUpdatedAt()         { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v)   { this.updatedAt = v; }
}