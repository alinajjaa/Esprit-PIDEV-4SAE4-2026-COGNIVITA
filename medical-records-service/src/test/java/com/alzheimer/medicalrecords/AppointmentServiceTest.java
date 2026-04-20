package com.alzheimer.medicalrecords;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;
import com.alzheimer.medicalrecords.service.*;
import com.alzheimer.medicalrecords.user.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Appointment & Reminder Logic Tests")
class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private NotificationService notificationService;
    @Mock private TimelineService timelineService;

    private AppointmentReminderJob reminderJob;

    @BeforeEach
    void setUp() {
        reminderJob = new AppointmentReminderJob(
                appointmentRepository,
                notificationService
        );
    }

    @Test
    @DisplayName("Reminder job sends email and marks reminderSent=true")
    void reminderJob_sendsEmailAndSetsFlag() {
        Appointment appt = scheduledAppointment(LocalDateTime.now().plusHours(24));
        appt.setReminderSent(false);
        MedicalRecord record = medicalRecord();
        appt.setMedicalRecord(record);

        when(appointmentRepository.findUpcomingForReminder(any(), any()))
                .thenReturn(List.of(appt));

        reminderJob.reminderJob();

        verify(notificationService).sendAppointmentReminder(eq(record), eq(appt));
        assertThat(appt.getReminderSent()).isTrue();
        verify(appointmentRepository).save(appt);
    }

    @Test
    @DisplayName("Reminder job skips appointments where reminderSent=true")
    void reminderJob_skipsAlreadyReminded() {
        when(appointmentRepository.findUpcomingForReminder(any(), any()))
                .thenReturn(Collections.emptyList());

        reminderJob.reminderJob();

        verify(notificationService, never()).sendAppointmentReminder(any(), any());
    }

    @Test
    @DisplayName("Auto-miss job marks overdue appointments as MISSED")
    void autoMissJob_marksOverdueAsMissed() {
        Appointment overdue = scheduledAppointment(LocalDateTime.now().minusHours(2));
        overdue.setStatus(AppointmentStatus.SCHEDULED);

        when(appointmentRepository.findOverdueScheduled(any()))
                .thenReturn(List.of(overdue));

        reminderJob.autoMissJob();

        assertThat(overdue.getStatus()).isEqualTo(AppointmentStatus.MISSED);
        verify(appointmentRepository).save(overdue);
    }

    @Test
    @DisplayName("Auto-miss job does nothing when no overdue appointments")
    void autoMissJob_noOverdue_doesNothing() {
        when(appointmentRepository.findOverdueScheduled(any()))
                .thenReturn(Collections.emptyList());

        reminderJob.autoMissJob();

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Appointment status transitions follow expected flow")
    void appointmentStatus_transitions() {
        Appointment appt = new Appointment();
        appt.setStatus(AppointmentStatus.SCHEDULED);

        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);

        appt.setStatus(AppointmentStatus.COMPLETED);
        appt.setCompletedAt(LocalDateTime.now());

        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(appt.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("AppointmentType has displayName for all types")
    void appointmentType_hasDisplayNames() {
        for (AppointmentType t : AppointmentType.values()) {
            assertThat(t.getDisplayName()).isNotBlank();
        }
    }

    @Test
    @DisplayName("AppointmentStatus has displayName for all statuses")
    void appointmentStatus_hasDisplayNames() {
        for (AppointmentStatus s : AppointmentStatus.values()) {
            assertThat(s.getDisplayName()).isNotBlank();
        }
    }

    @Test
    @DisplayName("Appointment createdAt is auto-set")
    void appointment_createdAt_autoSet() {
        Appointment appt = new Appointment();

        assertThat(appt.getCreatedAt()).isNotNull();
        assertThat(appt.getReminderSent()).isFalse();
    }

    // ── Helpers ─────────────────────────────────────────

    private Appointment scheduledAppointment(LocalDateTime scheduledAt) {
        Appointment appt = new Appointment();
        appt.setId(1L);
        appt.setScheduledAt(scheduledAt);
        appt.setStatus(AppointmentStatus.SCHEDULED);
        appt.setAppointmentType(AppointmentType.NEUROLOGIST);
        appt.setDoctorName("Dr. Smith");
        return appt;
    }

    private MedicalRecord medicalRecord() {
        User user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@test.com");

        MedicalRecord r = new MedicalRecord();
        r.setId(1L);
        r.setUser(user);

        return r;
    }
}