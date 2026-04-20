package com.alzheimer.medicalrecords.notification;

import com.alzheimer.medicalrecords.entity.MedicalRecord;
import com.alzheimer.medicalrecords.entity.RiskLevel;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.internet.MimeMessage;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    public NotificationService(NotificationRepository notificationRepository, JavaMailSender mailSender) {
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;
    }

    /**
     * Called after every risk score update. Creates an in-app notification
     * and sends an email when risk crosses HIGH or CRITICAL threshold.
     */
    @Transactional
    public void checkAndNotifyRiskChange(MedicalRecord record, RiskLevel previousLevel) {
        RiskLevel currentLevel = record.getRiskLevel();
        if (currentLevel == null) return;

        boolean escalated = isEscalation(previousLevel, currentLevel);
        if (!escalated && currentLevel != RiskLevel.CRITICAL && currentLevel != RiskLevel.HIGH) return;

        String title;
        String severity;
        if (currentLevel == RiskLevel.CRITICAL) {
            title = "🔴 CRITICAL Risk Alert — " + record.getUser().getFirstName() + " " + record.getUser().getLastName();
            severity = "CRITICAL";
        } else if (currentLevel == RiskLevel.HIGH) {
            title = "🟠 HIGH Risk Alert — " + record.getUser().getFirstName() + " " + record.getUser().getLastName();
            severity = "WARNING";
        } else {
            return;
        }

        String message = String.format(
            "Patient %s %s has a risk score of %.1f/100 (%s). Immediate clinical review is recommended.",
            record.getUser().getFirstName(), record.getUser().getLastName(),
            record.getRiskScore(), currentLevel.name()
        );

        Notification notif = new Notification();
        notif.setUserId(record.getUser().getId());
        notif.setMedicalRecordId(record.getId());
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setType("RISK_ALERT");
        notif.setSeverity(severity);

        // Try to send email
        boolean sent = false;
        try {
            sendAlertEmail(record, title, message);
            sent = true;
        } catch (Exception e) {
            System.err.println("[NotificationService] Email failed: " + e.getMessage());
        }
        notif.setEmailSent(sent);
        notificationRepository.save(notif);
    }

    @Transactional
    public void createMMSENotification(Long userId, Long recordId, String patientName, int score, String interpretation) {
        Notification notif = new Notification();
        notif.setUserId(userId);
        notif.setMedicalRecordId(recordId);
        notif.setTitle("📋 MMSE Test Completed — " + patientName);
        notif.setMessage(String.format("MMSE score: %d/30 — %s. Risk score has been updated.", score, interpretation));
        notif.setType("MMSE_RESULT");
        notif.setSeverity(score < 18 ? "WARNING" : score < 24 ? "INFO" : "INFO");
        notif.setEmailSent(false);
        notificationRepository.save(notif);
    }

    private boolean isEscalation(RiskLevel from, RiskLevel to) {
        if (from == null) return to == RiskLevel.HIGH || to == RiskLevel.CRITICAL;
        int[] order = {0, 1, 2, 3}; // LOW=0, MEDIUM=1, HIGH=2, CRITICAL=3
        int fromIdx = from.ordinal();
        int toIdx = to.ordinal();
        return toIdx > fromIdx;
    }

    private void sendAlertEmail(MedicalRecord record, String subject, String bodyText) throws Exception {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom("alzheimer.alerts@gmail.com");
        helper.setTo(record.getUser().getEmail());
        helper.setSubject(subject);
        helper.setText(buildEmailHtml(record, bodyText), true);
        mailSender.send(msg);
    }

    private String buildEmailHtml(MedicalRecord record, String bodyText) {
        String color = record.getRiskLevel() == RiskLevel.CRITICAL ? "#dc2626" : "#f97316";
        return "<div style='font-family:sans-serif;max-width:600px;margin:0 auto;'>"
            + "<div style='background:" + color + ";padding:20px;border-radius:8px 8px 0 0;'>"
            + "<h2 style='color:white;margin:0;'>⚠️ Alzheimer Risk Alert</h2></div>"
            + "<div style='background:#f9fafb;padding:24px;border:1px solid #e5e7eb;border-radius:0 0 8px 8px;'>"
            + "<p style='font-size:16px;color:#111827;'>" + bodyText + "</p>"
            + "<hr style='border:none;border-top:1px solid #e5e7eb;margin:16px 0;'/>"
            + "<p style='color:#6b7280;font-size:13px;'>This is an automated alert from the Alzheimer Risk Management System. "
            + "Please log in to the clinical dashboard to review this patient's full record.</p>"
            + "</div></div>";
    }
}
