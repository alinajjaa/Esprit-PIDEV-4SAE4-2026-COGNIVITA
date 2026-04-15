package louzaynej.pi.pi.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendRdvConfirmationHtml(String to,
                                        String patientName,
                                        String medecinName,
                                        LocalDateTime dateHeure) {

        DateTimeFormatter fmt = DateTimeFormatter
                .ofPattern("EEEE dd MMMM yyyy à HH:mm", Locale.FRENCH);

        String dateStr = (dateHeure != null) ? dateHeure.format(fmt) : "—";

        String subject = " Confirmation de votre rendez-vous";

        String html = buildHtml(patientName, medecinName, dateStr);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom("alouilouzeinej@gmail.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true); // true = HTML

            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    private String buildHtml(String patientName, String medecinName, String dateStr) {
        return """
<!doctype html>
<html lang="fr">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Confirmation RDV</title>
</head>
<body style="margin:0;padding:0;background:#f6f7fb;font-family:Arial,Helvetica,sans-serif;">
  <div style="max-width:640px;margin:0 auto;padding:24px;">
    <div style="background:#ffffff;border-radius:16px;padding:24px;border:1px solid #e9ecf5;">
      <div style="display:flex;align-items:center;gap:12px;">
        <div style="width:44px;height:44px;border-radius:12px;background:#e8fff3;display:flex;align-items:center;justify-content:center;">
          <span style="font-size:22px;"></span>
        </div>
        <div>
          <div style="font-size:18px;font-weight:700;color:#111827;">Rendez-vous confirmé</div>
          <div style="font-size:13px;color:#6b7280;margin-top:2px;">Merci de votre confiance.</div>
        </div>
      </div>

      <div style="margin-top:18px;font-size:14px;color:#111827;line-height:1.6;">
        Bonjour <b>%s</b>,<br/>
        Votre rendez-vous avec <b>%s</b> a été <b>confirmé</b>.
      </div>

      <div style="margin-top:18px;background:#f9fafb;border:1px solid #eef2f7;border-radius:14px;padding:16px;">
        <div style="font-size:13px;color:#6b7280;">Détails du rendez-vous</div>
        <div style="margin-top:10px;">
          <div style="display:flex;gap:10px;margin-bottom:8px;">
            <div style="width:120px;color:#6b7280;font-size:13px;">Médecin</div>
            <div style="font-size:14px;color:#111827;"><b>%s</b></div>
          </div>
          <div style="display:flex;gap:10px;">
            <div style="width:120px;color:#6b7280;font-size:13px;">Date & heure</div>
            <div style="font-size:14px;color:#111827;"><b>%s</b></div>
          </div>
        </div>
      </div>

      <div style="margin-top:18px;font-size:13px;color:#6b7280;line-height:1.6;">
        Si vous avez un empêchement, merci d’annuler ou de reprogrammer à l’avance.
      </div>

      <div style="margin-top:22px;border-top:1px solid #eef2f7;padding-top:14px;font-size:12px;color:#9ca3af;">
        Ceci est un message automatique. Merci de ne pas répondre.
      </div>
    </div>

    <div style="text-align:center;margin-top:14px;font-size:12px;color:#9ca3af;">
      © %d Clinique App
    </div>
  </div>
</body>
</html>
""".formatted(
                safe(patientName),
                safe(medecinName),
                safe(medecinName),
                safe(dateStr),
                java.time.Year.now().getValue()
        );
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}