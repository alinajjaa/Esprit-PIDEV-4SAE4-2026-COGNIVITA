package com.alzheimer.medicalrecords.entity;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;
import com.alzheimer.medicalrecords.service.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import com.alzheimer.medicalrecords.repository.AppointmentRepository;
import com.alzheimer.medicalrecords.service.NotificationService;
/**
 * Two scheduled jobs:
 *
 * 1. reminderJob   — runs every hour, sends 48h-ahead email reminders for SCHEDULED appointments
 * 2. autoMissJob   — runs every hour, marks past-due SCHEDULED appointments as MISSED
 */
@Component
public class AppointmentReminderJob {

    private static final Logger log = LoggerFactory.getLogger(AppointmentReminderJob.class);

    private final AppointmentRepository appointmentRepository;
    private final NotificationService   notificationService;

    public AppointmentReminderJob(AppointmentRepository appointmentRepository,
                                   @Qualifier("emailNotificationService") NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.notificationService   = notificationService;
    }

    /** Send 48-hour ahead reminders — runs every hour */
    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void reminderJob() {
        LocalDateTime now    = LocalDateTime.now();
        LocalDateTime window = now.plusHours(48);
        List<Appointment> due = appointmentRepository.findUpcomingForReminder(now, window);
        log.info("[REMINDER-JOB] Found {} appointments needing 48h reminder", due.size());
        for (Appointment appt : due) {
            try {
                notificationService.sendAppointmentReminder(appt.getMedicalRecord(), appt);
                appt.setReminderSent(true);
                appointmentRepository.save(appt);
                log.info("[REMINDER-JOB] Reminder sent for appointment #{} scheduled at {}",
                        appt.getId(), appt.getScheduledAt());
            } catch (Exception e) {
                log.error("[REMINDER-JOB] Failed for appointment #{}: {}", appt.getId(), e.getMessage());
            }
        }
    }

    /** Auto-mark past-due appointments as MISSED — runs every hour */
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 300_000)
    @Transactional
    public void autoMissJob() {
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> overdue = appointmentRepository.findOverdueScheduled(now);
        log.info("[MISS-JOB] Found {} overdue appointments to mark MISSED", overdue.size());
        for (Appointment appt : overdue) {
            appt.setStatus(AppointmentStatus.MISSED);
            appointmentRepository.save(appt);
            log.info("[MISS-JOB] Marked MISSED: appointment #{} was scheduled at {}",
                    appt.getId(), appt.getScheduledAt());
        }
    }
}
