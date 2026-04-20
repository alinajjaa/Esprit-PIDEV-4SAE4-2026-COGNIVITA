package com.alzheimer.notification.dto;

import java.time.LocalDateTime;

/** Fired by medication-adherence-service when a dose is missed beyond threshold */
public record MissedDoseEvent(
    Long medicationLogId,
    Long medicationId,
    Long patientUserId,
    String patientName,
    String patientEmail,
    String patientPhone,
    String medicationName,
    String dosage,
    String frequency,
    LocalDateTime scheduledTime,
    long hoursOverdue,
    String caregiverEmail,
    String caregiverPhone,
    String doctorEmail,
    String doctorPhone
) {}
