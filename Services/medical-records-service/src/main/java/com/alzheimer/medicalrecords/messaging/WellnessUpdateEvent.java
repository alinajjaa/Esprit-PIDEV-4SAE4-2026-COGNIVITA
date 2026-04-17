package com.alzheimer.medicalrecords.messaging;

import java.io.Serializable;

/**
 * Event received from health-prevention-service when a user's wellness profile
 * is created or updated.
 */
public class WellnessUpdateEvent implements Serializable {

    private Long   userId;
    private Long   medicalRecordId;
    private double wellnessScore;

    public WellnessUpdateEvent() {}

    public Long   getUserId()                          { return userId; }
    public void   setUserId(Long userId)               { this.userId = userId; }
    public Long   getMedicalRecordId()                 { return medicalRecordId; }
    public void   setMedicalRecordId(Long id)          { this.medicalRecordId = id; }
    public double getWellnessScore()                   { return wellnessScore; }
    public void   setWellnessScore(double wellnessScore){ this.wellnessScore = wellnessScore; }
}
