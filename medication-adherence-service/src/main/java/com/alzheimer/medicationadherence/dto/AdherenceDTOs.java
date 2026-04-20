package com.alzheimer.medicationadherence.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdherenceDTOs {

    /** Request: schedule a dose log entry */
    public record ScheduleDoseRequest(
        Long patientUserId,
        Long medicationId,
        String medicationName,
        String dosage,
        String frequency,
        LocalDateTime scheduledTime,
        String patientName,
        String patientEmail,
        String patientPhone,
        String caregiverEmail,
        String caregiverPhone,
        String doctorEmail,
        String doctorPhone
    ) {}

    /** Request: patient confirms a dose was taken */
    public record CheckInRequest(
        Long patientUserId,
        Long medicationId,
        LocalDate date,
        String notes
    ) {}

    /** Request: patient skips a dose */
    public record SkipDoseRequest(
        Long patientUserId,
        Long medicationId,
        LocalDate date,
        String reason
    ) {}

    /** Request: check drug interactions */
    public record DrugInteractionCheckRequest(
        java.util.List<String> medicationNames
    ) {}

    /** Response: adherence summary */
    public record AdherenceSummaryResponse(
        Long patientUserId,
        double weeklyScore,
        double monthlyScore,
        int streakDays,
        int totalScheduled,
        int totalTaken,
        int totalMissed,
        LocalDateTime lastCalculated,
        String adherenceLevel   // EXCELLENT (≥90), GOOD (≥70), FAIR (≥50), POOR (<50)
    ) {}
}
