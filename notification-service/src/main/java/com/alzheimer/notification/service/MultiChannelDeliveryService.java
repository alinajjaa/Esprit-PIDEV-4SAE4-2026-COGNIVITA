package com.alzheimer.notification.service;

import com.alzheimer.notification.entity.*;
import com.alzheimer.notification.repository.NotificationRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;

@Service
public class MultiChannelDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(MultiChannelDeliveryService.class);

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Value("${notification.email.from:alzheimer.alerts@gmail.com}")
    private String fromEmail;

    @Value("${twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${twilio.from-number:}")
    private String twilioFromNumber;

    @Value("${twilio.enabled:false}")
    private boolean twilioEnabled;

    public MultiChannelDeliveryService(NotificationRepository notificationRepository,
                                       JavaMailSender mailSender) {
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;
    }

    @PostConstruct
    public void initTwilio() {
        if (twilioEnabled && twilioAccountSid != null && !twilioAccountSid.isBlank()) {
            Twilio.init(twilioAccountSid, twilioAuthToken);
            log.info("[NotificationService] Twilio SMS initialized.");
        } else {
            log.info("[NotificationService] Twilio SMS disabled or not configured.");
        }
    }

    // ── Public send methods ──────────────────────────────────────────────────

    @Transactional
    public NotificationRecord sendEmail(Long userId, Long referenceId, NotificationType type,
                                         String severity, String title, String message,
                                         String recipientEmail, int escalationLevel,
                                         String escalationGroupId) {
        NotificationRecord record = buildRecord(userId, referenceId, type, NotificationChannel.EMAIL,
                severity, title, message, recipientEmail, null, escalationLevel, escalationGroupId);

        if (recipientEmail == null || recipientEmail.isBlank()) {
            record.setStatus(NotificationStatus.SKIPPED);
            record.setErrorMessage("No recipient email configured");
            return notificationRepository.save(record);
        }

        try {
            sendEmailInternal(recipientEmail, title, buildEmailHtml(title, message, severity));
            record.setStatus(NotificationStatus.SENT);
            record.setSentAt(LocalDateTime.now());
            log.info("[Email] Sent '{}' to {}", title, recipientEmail);
        } catch (Exception e) {
            record.setStatus(NotificationStatus.FAILED);
            record.setErrorMessage(e.getMessage());
            log.error("[Email] Failed to send to {}: {}", recipientEmail, e.getMessage());
        }
        return notificationRepository.save(record);
    }

    @Transactional
    public NotificationRecord sendSms(Long userId, Long referenceId, NotificationType type,
                                       String severity, String title, String smsBody,
                                       String recipientPhone, int escalationLevel,
                                       String escalationGroupId) {
        NotificationRecord record = buildRecord(userId, referenceId, type, NotificationChannel.SMS,
                severity, title, smsBody, null, recipientPhone, escalationLevel, escalationGroupId);

        if (!twilioEnabled || recipientPhone == null || recipientPhone.isBlank()) {
            record.setStatus(NotificationStatus.SKIPPED);
            record.setErrorMessage(twilioEnabled ? "No phone number" : "SMS channel disabled");
            return notificationRepository.save(record);
        }

        try {
            Message.creator(new PhoneNumber(recipientPhone),
                    new PhoneNumber(twilioFromNumber), smsBody).create();
            record.setStatus(NotificationStatus.SENT);
            record.setSentAt(LocalDateTime.now());
            log.info("[SMS] Sent to {}", recipientPhone);
        } catch (Exception e) {
            record.setStatus(NotificationStatus.FAILED);
            record.setErrorMessage(e.getMessage());
            log.error("[SMS] Failed to send to {}: {}", recipientPhone, e.getMessage());
        }
        return notificationRepository.save(record);
    }

    @Transactional
    public NotificationRecord saveInApp(Long userId, Long referenceId, NotificationType type,
                                         String severity, String title, String message) {
        NotificationRecord record = buildRecord(userId, referenceId, type, NotificationChannel.IN_APP,
                severity, title, message, null, null, 0, null);
        record.setStatus(NotificationStatus.SENT);
        record.setSentAt(LocalDateTime.now());
        return notificationRepository.save(record);
    }

    // ── Convenience: send all channels ───────────────────────────────────────

    public void deliverAll(Long userId, Long referenceId, NotificationType type, String severity,
                           String title, String message, String email, String phone,
                           int escalationLevel, String escalationGroupId) {
        saveInApp(userId, referenceId, type, severity, title, message);
        if (email != null && !email.isBlank()) {
            sendEmail(userId, referenceId, type, severity, title, message,
                    email, escalationLevel, escalationGroupId);
        }
        if (phone != null && !phone.isBlank()) {
            String smsText = "[Alzheimer Care] " + title + " — " + truncate(message, 140);
            sendSms(userId, referenceId, type, severity, title, smsText,
                    phone, escalationLevel, escalationGroupId);
        }
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private void sendEmailInternal(String to, String subject, String htmlBody) throws Exception {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(msg);
    }

    private String buildEmailHtml(String title, String body, String severity) {
        String headerColor = switch (severity != null ? severity.toUpperCase() : "INFO") {
            case "CRITICAL" -> "#dc2626";
            case "WARNING"  -> "#f97316";
            default         -> "#3b82f6";
        };
        return "<div style='font-family:sans-serif;max-width:600px;margin:0 auto;'>"
             + "<div style='background:" + headerColor + ";padding:20px;border-radius:8px 8px 0 0;'>"
             + "<h2 style='color:white;margin:0;'>⚠️ " + escapeHtml(title) + "</h2></div>"
             + "<div style='background:#f9fafb;padding:24px;border:1px solid #e5e7eb;border-radius:0 0 8px 8px;'>"
             + "<p style='font-size:16px;color:#111827;'>" + escapeHtml(body) + "</p>"
             + "<hr style='border:none;border-top:1px solid #e5e7eb;margin:16px 0;'/>"
             + "<p style='color:#6b7280;font-size:13px;'>Alzheimer Risk Management System — automated alert. "
             + "Please log in to the clinical dashboard to take action.</p>"
             + "</div></div>";
    }

    private NotificationRecord buildRecord(Long userId, Long referenceId, NotificationType type,
                                            NotificationChannel channel, String severity,
                                            String title, String message, String email, String phone,
                                            int escalationLevel, String escalationGroupId) {
        NotificationRecord r = new NotificationRecord();
        r.setUserId(userId);
        r.setReferenceId(referenceId);
        r.setNotificationType(type);
        r.setChannel(channel);
        r.setSeverity(severity);
        r.setTitle(title);
        r.setMessage(message);
        r.setRecipientEmail(email);
        r.setRecipientPhone(phone);
        r.setEscalationLevel(escalationLevel);
        r.setEscalationGroupId(escalationGroupId);
        r.setStatus(NotificationStatus.PENDING);
        return r;
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
