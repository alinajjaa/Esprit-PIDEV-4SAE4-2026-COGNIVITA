package com.alzheimer.medicalrecords.entity;

import com.alzheimer.medicalrecords.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "medical_records", indexes = {
    @Index(name = "idx_mr_user_id",        columnList = "user_id"),
    @Index(name = "idx_mr_risk_level",     columnList = "risk_level"),
    @Index(name = "idx_mr_gender",         columnList = "gender"),
    @Index(name = "idx_mr_family_history", columnList = "family_history"),
    @Index(name = "idx_mr_created_at",     columnList = "created_at DESC")
})
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Read-only mirror of the user_id FK — lets us look up the user even when
     *  the association is null (user not yet mirrored in local users table). */
    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @Column(name = "age")
    private Integer age;

    @Column(name = "gender", nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    // KEPT AS STRING — same as original
    @Column(name = "education_level", length = 100)
    private String educationLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "family_history")
    private FamilyHistory familyHistory = FamilyHistory.No;

    @Column(name = "current_symptoms", columnDefinition = "TEXT")
    private String currentSymptoms;

    @Column(name = "risk_factors", columnDefinition = "TEXT")
    private String riskFactors;

    @Column(name = "diagnosis_notes", columnDefinition = "TEXT")
    private String diagnosisNotes;

    // NEW: APOE gene status
    @Column(name = "apoe_status", length = 20)
    @Enumerated(EnumType.STRING)
    private APOEStatus apoeStatus = APOEStatus.NOT_TESTED;

    // NEW: Diagnosis stage (auto-suggested)
    @Column(name = "diagnosis_stage", length = 20)
    @Enumerated(EnumType.STRING)
    private DiagnosisStage diagnosisStage = DiagnosisStage.PRECLINICAL;

    @Column(name = "stage_manually_overridden")
    private Boolean stageManuallyOverridden = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }

    @Column(name = "risk_score")
    private Double riskScore = 0.0;

    @Column(name = "risk_level", length = 50)
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Column(name = "last_risk_calculation")
    private LocalDateTime lastRiskCalculation;

    @Column(name = "hereditary_risk_contribution")
    private Double hereditaryRiskContribution = 0.0;

    @Column(name = "wellness_risk_contribution")
    private Double wellnessRiskContribution = 0.0;

    /** Contribution from medication-adherence-service (0–15 points penalty for poor adherence) */
    @Column(name = "adherence_risk_contribution")
    private Double adherenceRiskContribution = 0.0;

    // ── Emergency / Notification contacts ────────────────────────────────────
    @Column(name = "caregiver_email", length = 200)
    private String caregiverEmail;

    @Column(name = "caregiver_phone", length = 30)
    private String caregiverPhone;

    @Column(name = "doctor_email", length = 200)
    private String doctorEmail;

    @Column(name = "doctor_phone", length = 30)
    private String doctorPhone;

    @OneToMany(mappedBy = "medicalRecord", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RiskFactor> riskFactorsList = new ArrayList<>();

    @OneToMany(mappedBy = "medicalRecord", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PreventionAction> preventionActions = new ArrayList<>();

    @OneToMany(mappedBy = "medicalRecord", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MedicalTimeline> timeline = new ArrayList<>();

    @OneToMany(mappedBy = "medicalRecord", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Medication> medications = new ArrayList<>();

    @OneToMany(mappedBy = "medicalRecord", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CareNote> careNotes = new ArrayList<>();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Long getUserId() { return userId; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }
    public FamilyHistory getFamilyHistory() { return familyHistory; }
    public void setFamilyHistory(FamilyHistory familyHistory) { this.familyHistory = familyHistory; }
    public String getCurrentSymptoms() { return currentSymptoms; }
    public void setCurrentSymptoms(String currentSymptoms) { this.currentSymptoms = currentSymptoms; }
    public String getRiskFactors() { return riskFactors; }
    public void setRiskFactors(String riskFactors) { this.riskFactors = riskFactors; }
    public String getDiagnosisNotes() { return diagnosisNotes; }
    public void setDiagnosisNotes(String diagnosisNotes) { this.diagnosisNotes = diagnosisNotes; }
    public APOEStatus getApoeStatus() { return apoeStatus; }
    public void setApoeStatus(APOEStatus apoeStatus) { this.apoeStatus = apoeStatus; }
    public DiagnosisStage getDiagnosisStage() { return diagnosisStage; }
    public void setDiagnosisStage(DiagnosisStage diagnosisStage) { this.diagnosisStage = diagnosisStage; }
    public Boolean getStageManuallyOverridden() { return stageManuallyOverridden; }
    public void setStageManuallyOverridden(Boolean b) { this.stageManuallyOverridden = b; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public LocalDateTime getLastRiskCalculation() { return lastRiskCalculation; }
    public void setLastRiskCalculation(LocalDateTime lastRiskCalculation) { this.lastRiskCalculation = lastRiskCalculation; }
    public Double getHereditaryRiskContribution() { return hereditaryRiskContribution; }
    public void setHereditaryRiskContribution(Double hereditaryRiskContribution) { this.hereditaryRiskContribution = hereditaryRiskContribution; }
    public Double getWellnessRiskContribution() { return wellnessRiskContribution; }
    public void setWellnessRiskContribution(Double wellnessRiskContribution) { this.wellnessRiskContribution = wellnessRiskContribution; }
    public Double getAdherenceRiskContribution() { return adherenceRiskContribution; }
    public void setAdherenceRiskContribution(Double adherenceRiskContribution) { this.adherenceRiskContribution = adherenceRiskContribution; }
    public String getCaregiverEmail() { return caregiverEmail; }
    public void setCaregiverEmail(String caregiverEmail) { this.caregiverEmail = caregiverEmail; }
    public String getCaregiverPhone() { return caregiverPhone; }
    public void setCaregiverPhone(String caregiverPhone) { this.caregiverPhone = caregiverPhone; }
    public String getDoctorEmail() { return doctorEmail; }
    public void setDoctorEmail(String doctorEmail) { this.doctorEmail = doctorEmail; }
    public String getDoctorPhone() { return doctorPhone; }
    public void setDoctorPhone(String doctorPhone) { this.doctorPhone = doctorPhone; }
    public List<RiskFactor> getRiskFactorsList() { return riskFactorsList; }
    public void setRiskFactorsList(List<RiskFactor> riskFactorsList) { this.riskFactorsList = riskFactorsList; }
    public List<PreventionAction> getPreventionActions() { return preventionActions; }
    public void setPreventionActions(List<PreventionAction> preventionActions) { this.preventionActions = preventionActions; }
    public List<MedicalTimeline> getTimeline() { return timeline; }
    public void setTimeline(List<MedicalTimeline> timeline) { this.timeline = timeline; }
    public List<Medication> getMedications() { return medications; }
    public void setMedications(List<Medication> medications) { this.medications = medications; }
    public List<CareNote> getCareNotes() { return careNotes; }
    public void setCareNotes(List<CareNote> careNotes) { this.careNotes = careNotes; }
}
