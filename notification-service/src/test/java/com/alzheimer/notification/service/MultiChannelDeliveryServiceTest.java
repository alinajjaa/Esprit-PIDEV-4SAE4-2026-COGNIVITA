package com.alzheimer.notification.service;

import com.alzheimer.notification.entity.*;
import com.alzheimer.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MultiChannelDeliveryService Unit Tests")
class MultiChannelDeliveryServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private MultiChannelDeliveryService service;

    @BeforeEach
    void setUp() {
        // Twilio disabled by default (field injected via @Value defaults)
    }

    // ── sendEmail ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("sendEmail: skips and saves SKIPPED when recipientEmail is null")
    void sendEmail_noEmail_skipped() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        NotificationRecord result = service.sendEmail(
                1L, 10L, NotificationType.MISSED_DOSE, "HIGH",
                "Missed Dose Alert", "Patient missed donepezil.",
                null, 0, null);

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SKIPPED);
        assertThat(result.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendEmail: saves FAILED when mail sender throws")
    void sendEmail_mailSenderThrows_failedStatus() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        NotificationRecord result = service.sendEmail(
                1L, 10L, NotificationType.MISSED_DOSE, "HIGH",
                "Alert", "Body", "test@example.com", 0, null);

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(result.getErrorMessage()).contains("SMTP error");
    }

    @Test
    @DisplayName("sendEmail: saves SENT on success")
    void sendEmail_success_sentStatus() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        NotificationRecord result = service.sendEmail(
                1L, 10L, NotificationType.RISK_ALERT, "WARNING",
                "Risk Alert", "High risk detected", "patient@example.com", 0, null);

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(result.getSentAt()).isNotNull();
    }

    // ── sendSms ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("sendSms: saves SKIPPED when Twilio disabled")
    void sendSms_twilioDisabled_skipped() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        NotificationRecord result = service.sendSms(
                1L, 10L, NotificationType.MISSED_DOSE, "HIGH",
                "Alert", "Body", "+21612345678", 0, null);

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SKIPPED);
        assertThat(result.getErrorMessage()).contains("SMS channel disabled");
    }

    @Test
    @DisplayName("sendSms: saves SKIPPED when phone number is blank")
    void sendSms_blankPhone_skipped() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        NotificationRecord result = service.sendSms(
                1L, 10L, NotificationType.MISSED_DOSE, "HIGH",
                "Alert", "Body", "", 0, null);

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SKIPPED);
    }

    // ── saveInApp ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("saveInApp: always saves with SENT status and IN_APP channel")
    void saveInApp_alwaysSent() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        NotificationRecord result = service.saveInApp(
                1L, 10L, NotificationType.APPOINTMENT_REMINDER, "INFO",
                "Appointment tomorrow", "You have an appointment tomorrow at 9am.");

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(result.getChannel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(result.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("saveInApp: sets correct userId and type")
    void saveInApp_setsFields() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        NotificationRecord result = service.saveInApp(
                5L, 20L, NotificationType.RISK_ALERT, "CRITICAL", "Title", "Message");

        assertThat(result.getUserId()).isEqualTo(5L);
        assertThat(result.getReferenceId()).isEqualTo(20L);
        assertThat(result.getNotificationType()).isEqualTo(NotificationType.RISK_ALERT);
        assertThat(result.getSeverity()).isEqualTo("CRITICAL");
    }
}
