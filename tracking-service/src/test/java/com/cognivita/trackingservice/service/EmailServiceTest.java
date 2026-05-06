package com.cognivita.trackingservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void sendAlertEmail_buildsAndSendsExpectedMessage() {
        EmailService emailService = new EmailService(mailSender, "sender@gmail.com");

        emailService.sendAlertEmail("receiver@gmail.com", "ALERT: OUT_OF_ZONE", "Patient left safe area");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals("sender@gmail.com", sentMessage.getFrom());
        assertEquals("receiver@gmail.com", sentMessage.getTo()[0]);
        assertEquals("ALERT: OUT_OF_ZONE", sentMessage.getSubject());
        assertEquals("Patient left safe area", sentMessage.getText());
    }
}
