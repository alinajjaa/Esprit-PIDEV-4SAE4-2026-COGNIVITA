package esprit.edu.userservice1.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendResetEmail(String to, String token) {
        String link = "http://localhost:4200/reset-password?token=" + token;
        String html = buildHtml(link);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("🔐 Reset your COGNIVITA password");
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String buildHtml(String link) {
        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'/></head>" +
                "<body style='margin:0;padding:0;background-color:#0a0a1a;font-family:Segoe UI,Arial,sans-serif;'>" +

                "<table width='100%%' cellpadding='0' cellspacing='0' style='background-color:#0a0a1a;padding:40px 20px;'>" +
                "<tr><td align='center'>" +
                "<table width='560' cellpadding='0' cellspacing='0' style='" +
                "background:linear-gradient(160deg,rgba(0,255,255,0.05) 0%%,rgba(255,0,255,0.05) 100%%);" +
                "border:1px solid rgba(0,255,255,0.3);border-radius:24px;overflow:hidden;" +
                "box-shadow:0 0 60px rgba(0,255,255,0.1);'>" +

                // Header
                "<tr><td align='center' style='" +
                "padding:48px 40px 32px;" +
                "background:linear-gradient(180deg,rgba(0,255,255,0.08) 0%%,transparent 100%%);" +
                "border-bottom:1px solid rgba(0,255,255,0.2);'>" +
                "<div style='font-size:3rem;margin-bottom:16px;'>🧠</div>" +
                "<h1 style='margin:0 0 8px;font-size:1.8rem;font-weight:900;letter-spacing:3px;color:#00ffff;'>COGNIVITA</h1>" +
                "<p style='margin:0;font-size:0.75rem;color:#93c5fd;letter-spacing:3px;text-transform:uppercase;'>Neural Identity System</p>" +
                "</td></tr>" +

                // Body
                "<tr><td style='padding:40px;'>" +
                "<h2 style='margin:0 0 12px;font-size:1.2rem;font-weight:700;color:#e2f4ff;letter-spacing:1px;'>Password Reset Request</h2>" +
                "<p style='margin:0 0 24px;color:#93c5fd;font-size:0.95rem;line-height:1.7;'>" +
                "We received a request to reset the password for your account. " +
                "Click the button below to create a new password. " +
                "This link will expire in <strong style='color:#00ffff;'>15 minutes</strong>." +
                "</p>" +

                // Divider
                "<div style='height:1px;background:linear-gradient(90deg,transparent,rgba(0,255,255,0.4),transparent);margin-bottom:32px;'></div>" +

                // Button
                "<table width='100%%' cellpadding='0' cellspacing='0'>" +
                "<tr><td align='center' style='padding-bottom:32px;'>" +
                "<a href='" + link + "' style='" +
                "display:inline-block;padding:16px 40px;" +
                "background:linear-gradient(135deg,rgba(0,255,255,0.2),rgba(255,0,255,0.2));" +
                "border:1px solid rgba(0,255,255,0.6);border-radius:12px;" +
                "color:#ffffff;font-size:0.85rem;font-weight:700;" +
                "letter-spacing:3px;text-transform:uppercase;text-decoration:none;'>" +
                "🔐 &nbsp; RESET MY PASSWORD" +
                "</a></td></tr></table>" +

                // Divider
                "<div style='height:1px;background:linear-gradient(90deg,transparent,rgba(0,255,255,0.2),transparent);margin-bottom:24px;'></div>" +

                // Fallback link
                "<p style='margin:0 0 8px;color:#93c5fd;font-size:0.82rem;'>If the button doesn't work, copy and paste this link:</p>" +
                "<p style='margin:0;padding:12px 16px;" +
                "background:rgba(0,255,255,0.05);border:1px solid rgba(0,255,255,0.2);" +
                "border-radius:8px;font-size:0.78rem;color:#00ffff;word-break:break-all;'>" +
                link +
                "</p>" +

                // Warning
                "<div style='margin-top:24px;padding:12px 16px;" +
                "background:rgba(255,50,80,0.07);border:1px solid rgba(255,50,80,0.25);" +
                "border-radius:8px;color:#ff6080;font-size:0.82rem;'>" +
                "⚠️ &nbsp; If you didn't request this, please ignore this email. Your password will remain unchanged." +
                "</div>" +
                "</td></tr>" +

                // Footer
                "<tr><td align='center' style='" +
                "padding:24px 40px;border-top:1px solid rgba(0,255,255,0.15);" +
                "color:rgba(147,197,253,0.4);font-size:0.75rem;letter-spacing:1px;'>" +
                "© 2026 COGNIVITA &nbsp;·&nbsp; This is an automated message" +
                "</td></tr>" +

                "</table></td></tr></table>" +
                "</body></html>";
    }

    public void sendOtpEmail(String to, String otp, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("🔐 COGNIVITA — Your verification code");
            helper.setFrom("najjaaali3@gmail.com");

            String html =
                    "<div style='font-family:Arial,sans-serif;background:#0a0a1a;padding:40px;'>" +
                            "<div style='max-width:480px;margin:0 auto;background:#111128;border:1px solid rgba(0,255,255,0.3);border-radius:16px;padding:40px;'>" +

                            // Header
                            "<div style='text-align:center;margin-bottom:32px;'>" +
                            "<div style='font-size:2.5rem;margin-bottom:12px;'>🧠</div>" +
                            "<h1 style='font-family:monospace;color:#00ffff;font-size:1.4rem;letter-spacing:4px;margin:0;'>COGNIVITA</h1>" +
                            "<p style='color:rgba(147,197,253,0.6);font-size:0.85rem;margin:8px 0 0;'>Neural Access Verification</p>" +
                            "</div>" +

                            // Greeting
                            "<p style='color:#e2f4ff;font-size:1rem;margin-bottom:24px;'>Hello <strong>" + fullName + "</strong>,</p>" +
                            "<p style='color:rgba(147,197,253,0.8);font-size:0.9rem;margin-bottom:32px;'>Your verification code is :</p>" +

                            // OTP Code
                            "<div style='text-align:center;margin:32px 0;'>" +
                            "<div style='display:inline-block;background:rgba(0,255,255,0.08);border:2px solid rgba(0,255,255,0.4);border-radius:16px;padding:20px 48px;'>" +
                            "<span style='font-family:monospace;font-size:2.5rem;font-weight:bold;color:#00ffff;letter-spacing:12px;'>" + otp + "</span>" +
                            "</div>" +
                            "</div>" +

                            // Expiry warning
                            "<div style='background:rgba(255,200,0,0.08);border:1px solid rgba(255,200,0,0.3);border-radius:10px;padding:14px;margin:24px 0;text-align:center;'>" +
                            "<p style='color:#ffd700;font-size:0.85rem;margin:0;'>⏱ This code expires in <strong>5 minutes</strong></p>" +
                            "</div>" +

                            // Warning
                            "<p style='color:rgba(147,197,253,0.5);font-size:0.78rem;text-align:center;margin-top:24px;'>" +
                            "If you didn't request this code, please ignore this email." +
                            "</p>" +

                            // Footer
                            "<div style='border-top:1px solid rgba(0,255,255,0.1);margin-top:32px;padding-top:20px;text-align:center;'>" +
                            "<p style='color:rgba(147,197,253,0.3);font-size:0.72rem;margin:0;'>© 2024 COGNIVITA — Secure Neural Platform</p>" +
                            "</div>" +

                            "</div></div>";

            helper.setText(html, true);
            mailSender.send(message);
            System.out.println("✅ OTP email sent to: " + to);

        } catch (Exception e) {
            System.err.println("❌ Failed to send OTP email: " + e.getMessage());
        }
    }

}