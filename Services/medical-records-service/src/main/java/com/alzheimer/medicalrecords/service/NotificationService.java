package com.alzheimer.medicalrecords.service;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * Sends email alerts when a patient's Alzheimer's risk level escalates.
 *
 * Bean name "emailNotificationService" avoids conflict with
 * notification.NotificationService (which handles in-app DB notifications).
 *
 * MedicalRecordController uses @Qualifier("emailNotificationService") to
 * inject exactly this bean.
 */
@Service("emailNotificationService")
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@cognivita.ai}")
    private String fromEmail;

    @Value("${report.email.doctor:doctor@clinic.com}")
    private String doctorEmail;

    private static final double ALERT_SCORE_THRESHOLD = 45.0;

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Called after every risk score update.
     * Sends email alerts on escalation or when risk crosses HIGH/CRITICAL threshold.
     */
    public void checkAndNotify(MedicalRecord record, RiskLevel previousLevel, String triggerContext) {
        if (record == null || record.getRiskLevel() == null) return;
        RiskLevel current = record.getRiskLevel();
        boolean escalated    = isEscalation(previousLevel, current);
        boolean aboveThreshold = record.getRiskScore() != null
                && record.getRiskScore() >= ALERT_SCORE_THRESHOLD;

        if (!escalated && !(aboveThreshold && (current == RiskLevel.HIGH || current == RiskLevel.CRITICAL))) {
            return;
        }

        String patientName = record.getUser() != null
                ? record.getUser().getFirstName() + " " + record.getUser().getLastName()
                : "Patient #" + record.getId();
        String patientEmail = record.getUser() != null ? record.getUser().getEmail() : null;

        String subject = buildSubject(current, patientName, record.getRiskScore());
        String doctorBody = buildDoctorBody(patientName, record, previousLevel, triggerContext, escalated);

        sendSilently(doctorEmail, subject, doctorBody);
        if (patientEmail != null && !patientEmail.isBlank()) {
            sendSilently(patientEmail, subject, buildPatientBody(patientName, record, triggerContext));
        }

        System.out.printf("[EMAIL-NOTIFICATION] Alert sent — patient=%s, prev=%s, current=%s, score=%.1f%n",
                patientName, previousLevel, current, record.getRiskScore());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isEscalation(RiskLevel from, RiskLevel to) {
        if (from == null || to == null) return false;
        return levelIndex(to) > levelIndex(from);
    }

    private int levelIndex(RiskLevel level) {
        return switch (level) {
            case LOW      -> 0;
            case MEDIUM   -> 1;
            case HIGH     -> 2;
            case CRITICAL -> 3;
        };
    }

    private String buildSubject(RiskLevel level, String name, Double score) {
        String emoji = level == RiskLevel.CRITICAL ? "🔴" : "🟠";
        return String.format("%s COGNIVITA Risk Alert — %s: %s (%.1f/100)",
                emoji, level.name(), name, score != null ? score : 0.0);
    }

    private String buildDoctorBody(String name, MedicalRecord record, RiskLevel prev,
                                    String trigger, boolean escalated) {
        String prevStr  = prev != null ? prev.name() : "N/A";
        String currStr  = record.getRiskLevel().name();
        String escalMsg = escalated
                ? "<p>⚠️ <strong>Risk escalated from " + prevStr + " → " + currStr + "</strong></p>"
                : "<p>Patient remains at <strong>" + currStr + "</strong> risk level.</p>";

        return wrap(
            "<p>Dear Medical Team,</p>" +
            "<p>Alzheimer's risk alert for patient <strong>" + esc(name) + "</strong>.</p>" +
            escalMsg +
            "<table style='border-collapse:collapse;width:100%;font-size:13px;margin:14px 0;'>" +
            row("Patient",       esc(name)) +
            row("Risk Score",    "<strong style='color:#dc2626;'>" + String.format("%.1f", record.getRiskScore() != null ? record.getRiskScore() : 0.0) + "/100</strong>") +
            row("Risk Level",    "<strong style='color:" + levelColor(record.getRiskLevel()) + ";'>" + currStr + "</strong>") +
            row("Previous Level", prevStr) +
            row("Trigger",       esc(trigger)) +
            "</table>" +
            "<p>Please review the patient dashboard in COGNIVITA.</p>"
        );
    }

    private String buildPatientBody(String name, MedicalRecord record, String trigger) {
        return wrap(
            "<p>Dear <strong>" + esc(name) + "</strong>,</p>" +
            "<div style='background:#fef2f2;border:2px solid #dc2626;border-radius:8px;padding:16px;margin:16px 0;'>" +
            "<h2 style='color:#dc2626;margin:0 0 8px;font-size:16px;'>⚠️ Health Risk Alert</h2>" +
            "<p style='margin:0;'>Your risk score is <strong style='color:#dc2626;font-size:18px;'>" +
            String.format("%.1f", record.getRiskScore() != null ? record.getRiskScore() : 0.0) +
            "/100</strong> (" + record.getRiskLevel().name() + " risk).</p>" +
            "</div>" +
            "<p>Please <strong>consult your physician</strong> as soon as possible.</p>"
        );
    }

    private String wrap(String content) {
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #e2e8f0;border-radius:8px;overflow:hidden;'>" +
               "<div style='background:#0f172a;padding:24px;text-align:center;'>" +
               "<h1 style='color:#00e5ff;margin:0;font-size:20px;'>🧠 COGNIVITA</h1>" +
               "<p style='color:#94a3b8;margin:5px 0 0;font-size:12px;'>Alzheimer's Detection &amp; Prevention System</p></div>" +
               "<div style='padding:24px;'>" + content +
               "<hr style='border:none;border-top:1px solid #e2e8f0;margin:18px 0;'/>" +
               "<p style='color:#94a3b8;font-size:11px;'>Automated alert from COGNIVITA. Do not reply.</p>" +
               "</div></div>";
    }

    private String row(String label, String value) {
        return "<tr><td style='padding:7px 12px;border:1px solid #e2e8f0;font-weight:600;background:#f8fafc;width:38%;'>" +
               label + "</td><td style='padding:7px 12px;border:1px solid #e2e8f0;'>" + value + "</td></tr>";
    }

    private String levelColor(RiskLevel level) {
        return switch (level) {
            case CRITICAL -> "#dc2626";
            case HIGH     -> "#f97316";
            case MEDIUM   -> "#f59e0b";
            case LOW      -> "#10b981";
        };
    }

    private String esc(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void sendSilently(String to, String subject, String htmlBody) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(msg);
            System.out.println("[EMAIL-NOTIFICATION] ✅ Sent → " + to);
        } catch (Exception e) {
            System.err.println("[EMAIL-NOTIFICATION] ❌ Failed → " + to + " : " + e.getMessage());
        }
    }

    // ── Appointment Notifications ─────────────────────────────────────────────

    /** Sends confirmation email when a new appointment is booked. */
    public void sendAppointmentConfirmation(MedicalRecord record, Appointment appt) {
        if (record == null || record.getUser() == null) return;
        String patientEmail = record.getUser().getEmail();
        String patientName  = record.getUser().getFirstName() + " " + record.getUser().getLastName();
        String subject = "✅ Appointment Confirmed — " + appt.getAppointmentType().getDisplayName()
                + " on " + appt.getScheduledAt().toLocalDate();

        String body = wrap(
            "<p>Dear <strong>" + esc(patientName) + "</strong>,</p>" +
            "<p>Your appointment has been confirmed:</p>" +
            buildApptTable(appt) +
            "<p>Please arrive 10 minutes early. Contact your clinic to reschedule if needed.</p>"
        );
        sendSilently(patientEmail, subject, body);
        sendSilently(doctorEmail,  subject,
            wrap("<p>New appointment scheduled for patient <strong>" + esc(patientName) + "</strong>.</p>" + buildApptTable(appt)));
    }

    /** Sends a 48-hour reminder. Called by AppointmentReminderJob. */
    public void sendAppointmentReminder(MedicalRecord record, Appointment appt) {
        if (record == null || record.getUser() == null) return;
        String patientEmail = record.getUser().getEmail();
        String patientName  = record.getUser().getFirstName() + " " + record.getUser().getLastName();
        String subject = "🔔 Reminder: " + appt.getAppointmentType().getDisplayName()
                + " tomorrow at " + appt.getScheduledAt().toLocalTime();

        String body = wrap(
            "<p>Dear <strong>" + esc(patientName) + "</strong>,</p>" +
            "<p>Reminder: you have an appointment <strong>tomorrow</strong>:</p>" +
            buildApptTable(appt) +
            "<p>Reply to this email or contact your clinic to cancel or reschedule.</p>"
        );
        sendSilently(patientEmail, subject, body);
    }

    private String buildApptTable(Appointment appt) {
        return "<table style='border-collapse:collapse;width:100%;font-size:13px;margin:14px 0;'>" +
            row("Type",     appt.getAppointmentType() != null ? appt.getAppointmentType().getDisplayName() : "") +
            row("Doctor",   appt.getDoctorName() != null ? esc(appt.getDoctorName()) : "—") +
            row("Date/Time", appt.getScheduledAt() != null ? appt.getScheduledAt().toString().replace("T", " ") : "—") +
            row("Location", appt.getLocation() != null ? esc(appt.getLocation()) : "—") +
            row("Notes",    appt.getNotes() != null ? esc(appt.getNotes()) : "—") +
            "</table>";
    }


}