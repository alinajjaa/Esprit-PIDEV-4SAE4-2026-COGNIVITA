package com.alzheimer.medicalrecords.report;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;
import com.alzheimer.medicalrecords.service.*;
import com.alzheimer.medicalrecords.user.User;
import com.alzheimer.medicalrecords.user.UserRepository;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generates complete medical PDF reports with automatic high-risk email alerts.
 * Family tree data is fetched from family-tree-service via HTTP (microservice pattern).
 */
@Service
public class PdfReportService {

    private static final String PATIENT_ALERT_EMAIL = "noreply@alzheimer.app";
    private static final double ALERT_THRESHOLD = 75.0;

    private final MedicalRecordRepository medicalRecordRepository;
    private final RiskFactorRepository riskFactorRepository;
    private final PreventionActionRepository preventionActionRepository;
    private final TimelineRepository timelineRepository;
    private final UserRepository userRepository;
    private final RiskScoreService riskScoreService;
    private final AppointmentRepository appointmentRepository;
    private final MedicationRepository medicationRepository;
    private final MedicationInteractionService medicationInteractionService;
    private final RiskTrendService riskTrendService;
    private final AuditService auditService;
    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate;

    @Value("${report.email.from:noreply@alzheimer.app}")
    private String fromEmail;

    @Value("${report.email.doctor:doctor@alzheimer.app}")
    private String doctorEmail;

    @Value("${family-tree.base-url:http://localhost:8083}")
    private String familyTreeBaseUrl;

    public PdfReportService(
            MedicalRecordRepository medicalRecordRepository,
            RiskFactorRepository riskFactorRepository,
            PreventionActionRepository preventionActionRepository,
            TimelineRepository timelineRepository,
            UserRepository userRepository,
            RiskScoreService riskScoreService,
            AppointmentRepository appointmentRepository,
            MedicationRepository medicationRepository,
            MedicationInteractionService medicationInteractionService,
            RiskTrendService riskTrendService,
            AuditService auditService,
            JavaMailSender mailSender,
            RestTemplate restTemplate) {
        this.medicalRecordRepository    = medicalRecordRepository;
        this.riskFactorRepository       = riskFactorRepository;
        this.preventionActionRepository = preventionActionRepository;
        this.timelineRepository         = timelineRepository;
        this.userRepository             = userRepository;
        this.riskScoreService           = riskScoreService;
        this.appointmentRepository      = appointmentRepository;
        this.medicationRepository       = medicationRepository;
        this.medicationInteractionService = medicationInteractionService;
        this.riskTrendService           = riskTrendService;
        this.auditService               = auditService;
        this.mailSender                 = mailSender;
        this.restTemplate               = restTemplate;
    }

    // ─── Public Entry Points ──────────────────────────────────────────────────

    @Transactional
    public byte[] generateReport(Long medicalRecordId) throws Exception {
        MedicalRecord record = medicalRecordRepository.findByIdWithUser(medicalRecordId)
                .orElseGet(() -> medicalRecordRepository.findById(medicalRecordId).orElse(null));
        if (record == null) throw new IllegalArgumentException("Medical Record ID " + medicalRecordId + " not found.");
        return doGenerate(record);
    }

    @Transactional
    public byte[] generateReportByUserId(Long userId) throws Exception {
        List<MedicalRecord> records = medicalRecordRepository.findTopByUserIdOrderByCreatedAtDesc(userId);
        MedicalRecord record = records.isEmpty() ? null : records.get(0);
        if (record == null) throw new IllegalArgumentException(
            "No medical record found for user ID " + userId + ". Please create a Medical Record first.");
        return doGenerate(record);
    }

    // ─── Core generation logic ────────────────────────────────────────────────

    private byte[] doGenerate(MedicalRecord record) throws Exception {
        User user = record.getUser();
        if (user == null && record.getUser() != null) user = userRepository.findById(record.getUser().getId()).orElse(null);
        if (user == null) user = buildPlaceholderUser(record.getId());

        riskScoreService.recalculateOnly(record);
        medicalRecordRepository.save(record);
        double liveScore = record.getRiskScore();

        Long recordId = record.getId();
        List<RiskFactor>       riskFactors  = riskFactorRepository.findByMedicalRecordId(recordId);
        List<PreventionAction> actions      = preventionActionRepository.findByMedicalRecordId(recordId);
        List<MedicalTimeline>  timeline     = timelineRepository.findByMedicalRecordIdOrderByEventDateDesc(recordId);
        List<Appointment>      appointments = appointmentRepository.findByMedicalRecordId(recordId);
        List<Medication>       medications  = medicationRepository.findByMedicalRecordId(recordId);
        List<MedicationInteractionService.InteractionAlert> interactions = medicationInteractionService.analyze(medications);
        Map<String, Object>    trendReport  = riskTrendService.getTrend(record.getId());

        // ── Fetch family tree data from family-tree-service via HTTP ──
        List<Map<String, Object>> familyMembers = fetchFamilyMembers(user.getId());
        Map<String, Object>       hereditary    = fetchHereditaryRisk(user.getId());

        byte[] pdf = buildPdf(record, user, riskFactors, actions, timeline,
                              familyMembers, hereditary, appointments, medications,
                              interactions, trendReport, liveScore);
        auditService.logPdfExported(recordId, user.getId());

        if (liveScore > ALERT_THRESHOLD) {
            sendHighRiskAlerts(user, record, pdf, liveScore);
        }
        return pdf;
    }

    // ─── HTTP calls to family-tree-service ───────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchFamilyMembers(Long userId) {
        try {
            String url = familyTreeBaseUrl + "/api/family-tree/user/" + userId;
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {});
            if (resp.getBody() != null && Boolean.TRUE.equals(resp.getBody().get("success"))) {
                Object data = resp.getBody().get("data");
                if (data instanceof List) return (List<Map<String, Object>>) data;
            }
        } catch (Exception e) {
            System.err.println("[PDF] Could not fetch family members from family-tree-service: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchHereditaryRisk(Long userId) {
        try {
            String url = familyTreeBaseUrl + "/api/family-tree/user/" + userId + "/risk-analysis";
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {});
            if (resp.getBody() != null && Boolean.TRUE.equals(resp.getBody().get("success"))) {
                Object data = resp.getBody().get("data");
                if (data instanceof Map) return (Map<String, Object>) data;
            }
        } catch (Exception e) {
            System.err.println("[PDF] Could not fetch hereditary risk from family-tree-service: " + e.getMessage());
        }
        return Collections.emptyMap();
    }

    // ─── PDF HTML Builder ─────────────────────────────────────────────────────

    private byte[] buildPdf(MedicalRecord record, User user,
                             List<RiskFactor> riskFactors,
                             List<PreventionAction> actions,
                             List<MedicalTimeline> timeline,
                             List<Map<String, Object>> familyMembers,
                             Map<String, Object> hereditaryRisk,
                             List<Appointment> appointments,
                             List<Medication> medications,
                             List<MedicationInteractionService.InteractionAlert> interactions,
                             Map<String, Object> trendReport,
                             double riskScore) throws Exception {

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
        String riskColor = riskScore >= 75 ? "#dc2626" : riskScore >= 50 ? "#d97706"
                         : riskScore >= 25 ? "#2563eb" : "#16a34a";
        String riskLabel = riskScore >= 75 ? "CRITICAL" : riskScore >= 50 ? "HIGH"
                         : riskScore >= 25 ? "MODERATE" : "LOW";

        StringBuilder h = new StringBuilder();
        h.append("<!DOCTYPE html><html><head><meta charset='UTF-8'/><style>");
        h.append("body{font-family:Arial,sans-serif;margin:0;padding:0;color:#1e293b;background:#fff;}");
        h.append(".cover{background:linear-gradient(135deg,#0f172a,#1e3a5f);color:#fff;padding:60px 50px;}");
        h.append(".cover h1{font-size:28px;margin:0 0 6px;color:#00e5ff;}");
        h.append(".cover p{font-size:13px;color:#94a3b8;margin:3px 0;}");
        h.append(".pname{font-size:22px;font-weight:bold;color:#fff;margin:18px 0 4px;}");
        h.append(".rdate{font-size:11px;color:#94a3b8;margin-top:6px;}");
        h.append(".rbadge{display:inline-block;padding:8px 22px;border-radius:20px;font-size:15px;font-weight:bold;color:#fff;margin-top:14px;}");
        h.append(".section{padding:28px 50px;border-bottom:1px solid #e2e8f0;}");
        h.append(".section h2{font-size:15px;color:#0f172a;border-left:4px solid #00e5ff;padding-left:12px;margin-bottom:14px;}");
        h.append("table{width:100%;border-collapse:collapse;font-size:12px;margin-top:8px;}");
        h.append("th{background:#0f172a;color:#fff;padding:8px 12px;text-align:left;font-size:11px;}");
        h.append("td{padding:7px 12px;border-bottom:1px solid #e2e8f0;font-size:12px;}");
        h.append("tr:nth-child(even) td{background:#f8fafc;}");
        h.append(".g2{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-bottom:12px;}");
        h.append(".ib{background:#f8fafc;border:1px solid #e2e8f0;border-radius:6px;padding:10px 14px;}");
        h.append(".il{font-size:10px;color:#64748b;text-transform:uppercase;letter-spacing:.5px;}");
        h.append(".iv{font-size:13px;font-weight:600;color:#1e293b;margin-top:2px;}");
        h.append(".rm{background:#e2e8f0;border-radius:8px;height:14px;overflow:hidden;}");
        h.append(".rf{height:14px;border-radius:8px;color:#fff;font-size:10px;line-height:14px;padding-left:8px;}");
        h.append(".alert{background:#fef2f2;border:2px solid #dc2626;border-radius:8px;padding:16px;margin:4px 0 16px;}");
        h.append(".alert h3{color:#dc2626;margin:0 0 8px;font-size:15px;}");
        h.append(".footer{background:#0f172a;color:#64748b;padding:18px 50px;font-size:10px;text-align:center;}");
        h.append("</style></head><body>");

        // Cover
        h.append("<div class='cover'>");
        h.append("<h1>&#129504; COGNIVITA &mdash; Medical Report</h1>");
        h.append("<p>Alzheimer's Detection &amp; Prevention System</p>");
        h.append("<div class='pname'>").append(esc(user.getFirstName())).append(" ").append(esc(user.getLastName())).append("</div>");
        h.append("<p>").append(esc(user.getEmail())).append("</p>");
        h.append("<div class='rdate'>Medical Record #").append(record.getId())
         .append(" &nbsp;&bull;&nbsp; Generated: ").append(now).append("</div>");
        h.append("<div class='rbadge' style='background:").append(riskColor).append(";'>")
         .append("Risk Score: ").append(String.format("%.1f", riskScore)).append("% &mdash; ").append(riskLabel)
         .append("</div></div>");

        // High-risk alert banner
        if (riskScore > ALERT_THRESHOLD) {
            h.append("<div class='section'><div class='alert'><h3>&#9888; HIGH RISK ALERT &mdash; Score: ")
             .append(String.format("%.1f", riskScore)).append("%</h3>")
             .append("<p style='margin:0;font-size:13px;'>This patient's Alzheimer's risk exceeds the ")
             .append((int) ALERT_THRESHOLD).append("% critical threshold. Immediate specialist referral recommended.</p>")
             .append("</div></div>");
        }

        // Patient summary
        h.append("<div class='section'><h2>Patient Summary</h2><div class='g2'>");
        ib(h, "Patient Name", esc(user.getFirstName()) + " " + esc(user.getLastName()));
        ib(h, "Email",        esc(user.getEmail()));
        ib(h, "Age",          record.getAge() != null ? record.getAge() + " years" : "N/A");
        ib(h, "Gender",       record.getGender() != null ? record.getGender().name() : "N/A");
        ib(h, "Risk Level",   record.getRiskLevel() != null ? record.getRiskLevel().name() : "N/A");
        ib(h, "Generated",    now);
        h.append("</div>");
        h.append("<div style='margin-top:10px;'>");
        h.append("<p style='font-size:12px;margin:0 0 4px;'>Risk Score: <strong>")
         .append(String.format("%.1f", riskScore)).append("%</strong></p>");
        h.append("<div class='rm'><div class='rf' style='width:")
         .append(Math.min(99, riskScore)).append("%;background:").append(riskColor).append(";'>")
         .append(String.format("%.1f", riskScore)).append("%</div></div></div></div>");

        // Family history section (data from family-tree-service)
        h.append("<div class='section'><h2>Family History &amp; Hereditary Risk</h2>");
        if (!familyMembers.isEmpty()) {
            double hScore = hereditaryRisk.get("hereditaryRiskScore") != null
                    ? ((Number) hereditaryRisk.get("hereditaryRiskScore")).doubleValue() : 0;
            h.append("<div class='g2'>");
            ib(h, "Hereditary Score", String.format("%.1f%%", hScore));
            ib(h, "Risk Level",       String.valueOf(hereditaryRisk.getOrDefault("riskLevel", "N/A")));
            ib(h, "Total Members",    String.valueOf(hereditaryRisk.getOrDefault("totalFamilyMembers", 0)));
            ib(h, "Affected",         String.valueOf(hereditaryRisk.getOrDefault("affectedMembers", 0)));
            h.append("</div>");
            h.append("<table><tr><th>Name</th><th>Relationship</th><th>Alzheimer's</th><th>Dementia</th><th>Age</th></tr>");
            for (Map<String, Object> fm : familyMembers) {
                String rel = fm.get("relationship") != null ? fm.get("relationship").toString().replace("_", " ") : "";
                boolean alz = Boolean.TRUE.equals(fm.get("hasAlzheimers"));
                boolean dem = Boolean.TRUE.equals(fm.get("hasDementia"));
                h.append("<tr><td>").append(nvl(fm.get("fullName"))).append("</td>")
                 .append("<td>").append(rel).append("</td>")
                 .append("<td>").append(alz ? "<strong style='color:#dc2626;'>YES</strong>" : "No").append("</td>")
                 .append("<td>").append(dem ? "<strong style='color:#d97706;'>YES</strong>" : "No").append("</td>")
                 .append("<td>").append(fm.get("age") != null ? fm.get("age") + "" : "N/A").append("</td></tr>");
            }
            h.append("</table>");
        } else {
            h.append("<p style='color:#64748b;font-size:12px;'>No family members recorded in the family tree module.</p>");
        }
        h.append("</div>");

        // Risk factors
        h.append("<div class='section'><h2>Clinical Risk Factors (").append(riskFactors.size()).append(")</h2>");
        if (!riskFactors.isEmpty()) {
            h.append("<table><tr><th>Factor</th><th>Severity</th><th>Status</th><th>Notes</th></tr>");
            for (RiskFactor rf : riskFactors) {
                h.append("<tr><td>").append(nvl(rf.getFactorType())).append("</td>")
                 .append("<td>").append(rf.getSeverity() != null ? rf.getSeverity().name() : "").append("</td>")
                 .append("<td>").append(Boolean.TRUE.equals(rf.getIsActive()) ? "Active" : "Resolved").append("</td>")
                 .append("<td>").append(nvl(rf.getNotes())).append("</td></tr>");
            }
            h.append("</table>");
        } else {
            h.append("<p style='color:#64748b;font-size:12px;'>No risk factors recorded.</p>");
        }
        h.append("</div>");

        // Appointments
        h.append("<div class='section'><h2>Appointments (").append(appointments.size()).append(")</h2>");
        if (!appointments.isEmpty()) {
            h.append("<table><tr><th>Type</th><th>Doctor</th><th>Date</th><th>Status</th></tr>");
            for (Appointment a : appointments) {
                h.append("<tr><td>").append(a.getAppointmentType() != null ? a.getAppointmentType().getDisplayName() : "").append("</td>")
                 .append("<td>").append(nvl(a.getDoctorName())).append("</td>")
                 .append("<td>").append(a.getScheduledAt() != null ? a.getScheduledAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) : "").append("</td>")
                 .append("<td>").append(a.getStatus() != null ? a.getStatus().getDisplayName() : "").append("</td></tr>");
            }
            h.append("</table>");
        } else {
            h.append("<p style='color:#64748b;font-size:12px;'>No appointments recorded.</p>");
        }
        h.append("</div>");

        // Timeline
        h.append("<div class='section'><h2>Medical Timeline (").append(timeline.size()).append(" events)</h2>");
        if (!timeline.isEmpty()) {
            h.append("<table><tr><th>Date</th><th>Event</th><th>Description</th></tr>");
            for (int i = 0; i < Math.min(20, timeline.size()); i++) {
                MedicalTimeline t = timeline.get(i);
                h.append("<tr><td>").append(t.getEventDate() != null ? t.getEventDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) : "N/A").append("</td>")
                 .append("<td>").append(t.getEventType() != null ? t.getEventType().name().replace("_", " ") : "").append("</td>")
                 .append("<td>").append(nvl(t.getDescription())).append("</td></tr>");
            }
            h.append("</table>");
        } else {
            h.append("<p style='color:#64748b;font-size:12px;'>No timeline events recorded.</p>");
        }
        h.append("</div>");

        h.append("<div class='footer'>COGNIVITA Alzheimer Detection System &bull; Confidential &bull; ").append(now).append("</div>");
        h.append("</body></html>");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        com.itextpdf.html2pdf.HtmlConverter.convertToPdf(h.toString(), out);
        return out.toByteArray();
    }

    // ─── Email Alerts ─────────────────────────────────────────────────────────

    private void sendHighRiskAlerts(User user, MedicalRecord record, byte[] pdf, double score) {
        String name     = user.getFirstName() + " " + user.getLastName();
        String filename = "Medical_Report_" + name.replace(" ", "_") + ".pdf";
        try { sendEmail(PATIENT_ALERT_EMAIL, patientBody(name, score), filename, pdf, score); }
        catch (Exception e) { System.err.println("[PDF] Patient email failed: " + e.getMessage()); }
        try { sendEmail(doctorEmail, doctorBody(name, score, record), filename, pdf, score); }
        catch (Exception e) { System.err.println("[PDF] Doctor email failed: " + e.getMessage()); }
    }

    private void sendEmail(String to, String html, String filename, byte[] pdf, double score) throws Exception {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
        h.setFrom(fromEmail);
        h.setTo(to);
        h.setSubject("⚠️ COGNIVITA — High Risk Alert: " + String.format("%.1f%%", score));
        h.setText(html, true);
        h.addAttachment(filename, new org.springframework.core.io.ByteArrayResource(pdf), "application/pdf");
        mailSender.send(msg);
    }

    private String patientBody(String name, double score) {
        return wrap("<p>Dear <strong>" + esc(name) + "</strong>,</p>" +
            "<p>Your Alzheimer's risk score is <strong style='color:#dc2626;'>" +
            String.format("%.1f", score) + "%</strong>, exceeding the alert threshold. " +
            "Please consult your neurologist as soon as possible.</p>" +
            "<p>Your full medical report is attached.</p>");
    }

    private String doctorBody(String patient, double score, MedicalRecord record) {
        return wrap("<p>Patient <strong>" + esc(patient) + "</strong> has triggered a high-risk alert: " +
            "<strong style='color:#dc2626;'>" + String.format("%.1f", score) + "%</strong></p>" +
            "<p>Age: " + record.getAge() + " | Risk Level: " +
            (record.getRiskLevel() != null ? record.getRiskLevel().name() : "N/A") + "</p>" +
            "<p>Full medical report attached.</p>");
    }

    private String wrap(String content) {
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;'>" +
               "<div style='background:#0f172a;padding:24px;text-align:center;'>" +
               "<h1 style='color:#00e5ff;margin:0;'>&#129504; COGNIVITA</h1></div>" +
               "<div style='padding:24px;'>" + content + "</div></div>";
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void ib(StringBuilder h, String label, String value) {
        h.append("<div class='ib'><div class='il'>").append(label).append("</div>")
         .append("<div class='iv'>").append(value).append("</div></div>");
    }

    private User buildPlaceholderUser(Long id) {
        User u = new User();
        u.setId(id);
        u.setFirstName("Patient");
        u.setLastName("#" + id);
        u.setEmail(PATIENT_ALERT_EMAIL);
        u.setPassword("");
        return u;
    }

    private String nvl(Object v) { return (v != null && !v.toString().isEmpty()) ? v.toString() : "\u2014"; }
    private String nvl(String v) { return (v != null && !v.isEmpty()) ? v : "\u2014"; }
    private String esc(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
