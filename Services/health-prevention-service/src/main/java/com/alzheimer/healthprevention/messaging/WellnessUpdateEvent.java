package com.alzheimer.healthprevention.messaging;

import java.io.Serializable;

/**
 * Event published when a user's wellness profile is created or updated.
 * Carries the wellness risk contribution score to be applied in medical-records-service.
 */
public class WellnessUpdateEvent implements Serializable {

    private Long   userId;
    private Long   medicalRecordId;   // null if not known
    private double wellnessScore;

    public WellnessUpdateEvent() {}

    public WellnessUpdateEvent(Long userId, Long medicalRecordId, double wellnessScore) {
        this.userId           = userId;
        this.medicalRecordId  = medicalRecordId;
        this.wellnessScore    = wellnessScore;
    }

    public Long   getUserId()                          { return userId; }
    public void   setUserId(Long userId)               { this.userId = userId; }
    public Long   getMedicalRecordId()                 { return medicalRecordId; }
    public void   setMedicalRecordId(Long id)          { this.medicalRecordId = id; }
    public double getWellnessScore()                   { return wellnessScore; }
    public void   setWellnessScore(double wellnessScore){ this.wellnessScore = wellnessScore; }
}
