package com.cognivita.dashboardservice.dto;

public class PatientSafetyBurdenDto {

    private final Long patientId;
    private final Long totalAlerts;
    private final Long unreadAlerts;
    private final Long noMovementCount;
    private final Long outOfZoneCount;
    private final Long riskScore;

    public PatientSafetyBurdenDto(Long patientId,
                                  Long totalAlerts,
                                  Long unreadAlerts,
                                  Long noMovementCount,
                                  Long outOfZoneCount,
                                  Long riskScore) {
        this.patientId = patientId;
        this.totalAlerts = totalAlerts;
        this.unreadAlerts = unreadAlerts;
        this.noMovementCount = noMovementCount;
        this.outOfZoneCount = outOfZoneCount;
        this.riskScore = riskScore;
    }

    public Long getPatientId() {
        return patientId;
    }

    public Long getTotalAlerts() {
        return totalAlerts;
    }

    public Long getUnreadAlerts() {
        return unreadAlerts;
    }

    public Long getNoMovementCount() {
        return noMovementCount;
    }

    public Long getOutOfZoneCount() {
        return outOfZoneCount;
    }

    public Long getRiskScore() {
        return riskScore;
    }
}
