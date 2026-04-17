package com.alzheimer.medicalrecords.dto;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.service.*;
import com.alzheimer.medicalrecords.user.*;
import com.alzheimer.medicalrecords.mmse.*;

import java.time.LocalDateTime;
import java.util.List;

public class MedicalRecordDTO {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private Integer age;
    private String gender;
    private String educationLevel;
    private String familyHistory;
    private String riskFactors;
    private String currentSymptoms;
    private String diagnosisNotes;
    private Double riskScore;
    private String riskLevel;
    private LocalDateTime lastRiskCalculation;
    private Double hereditaryRiskContribution;
    private Double wellnessRiskContribution;
    private List<String> recommendations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // NEW fields - safely nullable
    private String apoeStatus;
    private String apoeStatusDisplay;
    private String diagnosisStage;
    private String diagnosisStageDisplay;
    private String diagnosisStageDescription;
    private Boolean stageManuallyOverridden;

    public MedicalRecordDTO() {}

    public MedicalRecordDTO(MedicalRecord record) {
        // ORIGINAL constructor - untouched
        this.id = record.getId();
        this.userId = record.getUser().getId();
        this.userName = record.getUser().getFirstName() + " " + record.getUser().getLastName();
        this.userEmail = record.getUser().getEmail();
        this.age = record.getAge();
        this.gender = record.getGender() != null ? record.getGender().name() : null;
        this.educationLevel = record.getEducationLevel();
        this.familyHistory = record.getFamilyHistory() != null ? record.getFamilyHistory().name() : null;
        this.riskFactors = record.getRiskFactors();
        this.currentSymptoms = record.getCurrentSymptoms();
        this.diagnosisNotes = record.getDiagnosisNotes();
        this.riskScore = record.getRiskScore();
        this.riskLevel = record.getRiskLevel() != null ? record.getRiskLevel().name() : "LOW";
        this.lastRiskCalculation = record.getLastRiskCalculation();
        this.hereditaryRiskContribution = record.getHereditaryRiskContribution();
        this.wellnessRiskContribution = record.getWellnessRiskContribution();
        this.createdAt = record.getCreatedAt();
        this.updatedAt = record.getUpdatedAt();
        // NEW fields - safely set with null checks
        if (record.getApoeStatus() != null) {
            this.apoeStatus = record.getApoeStatus().name();
            this.apoeStatusDisplay = record.getApoeStatus().getDisplayName();
        }
        if (record.getDiagnosisStage() != null) {
            this.diagnosisStage = record.getDiagnosisStage().name();
            this.diagnosisStageDisplay = record.getDiagnosisStage().getDisplayName();
            this.diagnosisStageDescription = record.getDiagnosisStage().getDescription();
        }
        this.stageManuallyOverridden = record.getStageManuallyOverridden();
    }

    public MedicalRecordDTO(MedicalRecord record, List<String> recommendations) {
        this(record);
        this.recommendations = recommendations;
    }

    // Getters and Setters - ORIGINAL
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }
    public String getFamilyHistory() { return familyHistory; }
    public void setFamilyHistory(String familyHistory) { this.familyHistory = familyHistory; }
    public String getRiskFactors() { return riskFactors; }
    public void setRiskFactors(String riskFactors) { this.riskFactors = riskFactors; }
    public String getCurrentSymptoms() { return currentSymptoms; }
    public void setCurrentSymptoms(String currentSymptoms) { this.currentSymptoms = currentSymptoms; }
    public String getDiagnosisNotes() { return diagnosisNotes; }
    public void setDiagnosisNotes(String diagnosisNotes) { this.diagnosisNotes = diagnosisNotes; }
    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public LocalDateTime getLastRiskCalculation() { return lastRiskCalculation; }
    public void setLastRiskCalculation(LocalDateTime lastRiskCalculation) { this.lastRiskCalculation = lastRiskCalculation; }
    public Double getHereditaryRiskContribution() { return hereditaryRiskContribution; }
    public void setHereditaryRiskContribution(Double hereditaryRiskContribution) { this.hereditaryRiskContribution = hereditaryRiskContribution; }
    public Double getWellnessRiskContribution() { return wellnessRiskContribution; }
    public void setWellnessRiskContribution(Double wellnessRiskContribution) { this.wellnessRiskContribution = wellnessRiskContribution; }
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    // NEW getters/setters
    public String getApoeStatus() { return apoeStatus; }
    public void setApoeStatus(String apoeStatus) { this.apoeStatus = apoeStatus; }
    public String getApoeStatusDisplay() { return apoeStatusDisplay; }
    public void setApoeStatusDisplay(String apoeStatusDisplay) { this.apoeStatusDisplay = apoeStatusDisplay; }
    public String getDiagnosisStage() { return diagnosisStage; }
    public void setDiagnosisStage(String diagnosisStage) { this.diagnosisStage = diagnosisStage; }
    public String getDiagnosisStageDisplay() { return diagnosisStageDisplay; }
    public void setDiagnosisStageDisplay(String diagnosisStageDisplay) { this.diagnosisStageDisplay = diagnosisStageDisplay; }
    public String getDiagnosisStageDescription() { return diagnosisStageDescription; }
    public void setDiagnosisStageDescription(String diagnosisStageDescription) { this.diagnosisStageDescription = diagnosisStageDescription; }
    public Boolean getStageManuallyOverridden() { return stageManuallyOverridden; }
    public void setStageManuallyOverridden(Boolean stageManuallyOverridden) { this.stageManuallyOverridden = stageManuallyOverridden; }
}
