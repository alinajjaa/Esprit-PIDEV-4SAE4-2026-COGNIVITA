package com.alzheimer.notification.dto;

/** Fired by medical-records-service when risk score crosses HIGH/CRITICAL threshold */
public record RiskAlertEvent(
    Long medicalRecordId,
    Long patientUserId,
    String patientName,
    String patientEmail,
    String patientPhone,
    double riskScore,
    String riskLevel,           // LOW | MEDIUM | HIGH | CRITICAL
    String previousRiskLevel,
    String caregiverEmail,
    String caregiverPhone,
    String doctorEmail,
    String doctorPhone
) {}
