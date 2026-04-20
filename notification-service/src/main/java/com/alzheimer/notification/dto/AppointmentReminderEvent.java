package com.alzheimer.notification.dto;

import java.time.LocalDateTime;

/** Fired by medical-records-service appointment scheduler */
public record AppointmentReminderEvent(
    Long appointmentId,
    Long patientUserId,
    String patientName,
    String patientEmail,
    String patientPhone,
    String doctorName,
    String appointmentType,
    LocalDateTime appointmentDateTime,
    String location,
    int hoursUntilAppointment
) {}
