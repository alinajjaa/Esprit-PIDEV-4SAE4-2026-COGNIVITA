package louzaynej.pi.pi;


import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import louzaynej.pi.pi.services.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setup() {
        emailService = new EmailService(mailSender);
    }

    @Test
    void sendRdvConfirmationHtml_whenOk_callsSend() {
        // Arrange: real MimeMessage (no SMTP needed)
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailService.sendRdvConfirmationHtml(
                "ali@test.com",
                "Ali",
                "Dr House",
                LocalDateTime.of(2026, 2, 25, 10, 0)
        );

        // Assert
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendRdvConfirmationHtml_whenCreateMimeMessageFails_throwsRuntimeException() {
        // Arrange
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("boom"));

        // Act + Assert
        assertThrows(RuntimeException.class, () ->
                emailService.sendRdvConfirmationHtml(
                        "ali@test.com",
                        "Ali",
                        "Dr House",
                        LocalDateTime.now()
                )
        );

        verify(mailSender).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}