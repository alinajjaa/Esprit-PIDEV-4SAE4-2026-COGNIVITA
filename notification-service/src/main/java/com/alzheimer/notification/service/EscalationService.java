package com.alzheimer.notification.service;

import com.alzheimer.notification.entity.*;
import com.alzheimer.notification.repository.EscalationRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class EscalationService {

    private static final Logger log = LoggerFactory.getLogger(EscalationService.class);

    private final EscalationRuleRepository escalationRuleRepository;
    private final MultiChannelDeliveryService deliveryService;

    public EscalationService(EscalationRuleRepository escalationRuleRepository,
                              MultiChannelDeliveryService deliveryService) {
        this.escalationRuleRepository = escalationRuleRepository;
        this.deliveryService = deliveryService;
    }

    /**
     * Create a new escalation tracking entry when a missed-dose or risk event fires.
     * Idempotent: if an active rule already exists for this patient + type + reference, skip.
     */
    @Transactional
    public EscalationRule createEscalation(Long patientUserId, EscalationType type, Long referenceId,
                                            String contextMessage,
                                            String caregiverEmail, String caregiverPhone,
                                            String doctorEmail, String doctorPhone) {
        Optional<EscalationRule> existing = escalationRuleRepository
                .findByPatientUserIdAndEscalationTypeAndReferenceIdAndActiveTrue(
                        patientUserId, type, referenceId);
        if (existing.isPresent()) {
            log.debug("[Escalation] Already active for patient {} type {} ref {}", patientUserId, type, referenceId);
            return existing.get();
        }

        EscalationRule rule = new EscalationRule();
        rule.setPatientUserId(patientUserId);
        rule.setEscalationType(type);
        rule.setReferenceId(referenceId);
        rule.setContextMessage(contextMessage);
        rule.setCaregiverEmail(caregiverEmail);
        rule.setCaregiverPhone(caregiverPhone);
        rule.setDoctorEmail(doctorEmail);
        rule.setDoctorPhone(doctorPhone);
        rule.setCurrentLevel(0);
        rule.setActive(true);
        log.info("[Escalation] Created: patient={} type={} ref={}", patientUserId, type, referenceId);
        return escalationRuleRepository.save(rule);
    }

    /**
     * Advance a rule to the caregiver level (level 1).
     * If no caregiver contact is configured, skips directly to doctor.
     */
    @Transactional
    public void escalateToCaregiver(EscalationRule rule) {
        if (rule.getCaregiverEmail() == null && rule.getCaregiverPhone() == null) {
            log.info("[Escalation] No caregiver contact for patient {} — skipping to doctor", rule.getPatientUserId());
            escalateToDoctor(rule);
            return;
        }
        String title   = "⚠️ Patient Alert — Caregiver Notification";
        String message = "Patient requires attention. " + rule.getContextMessage();
        deliveryService.deliverAll(
                rule.getPatientUserId(), rule.getReferenceId(),
                NotificationType.CAREGIVER_ESCALATION, "WARNING",
                title, message,
                rule.getCaregiverEmail(), rule.getCaregiverPhone(),
                1, "esc-" + rule.getId()
        );
        rule.setCurrentLevel(1);
        rule.setLastEscalatedAt(LocalDateTime.now());
        escalationRuleRepository.save(rule);
        log.info("[Escalation] Level 1 (caregiver) fired for patient {}", rule.getPatientUserId());
    }

    /**
     * Advance a rule to the doctor level (level 2) and mark the chain complete.
     */
    @Transactional
    public void escalateToDoctor(EscalationRule rule) {
        if (rule.getDoctorEmail() == null && rule.getDoctorPhone() == null) {
            log.warn("[Escalation] No doctor contact for patient {} — escalation chain exhausted", rule.getPatientUserId());
            rule.setActive(false);
            rule.setResolvedAt(LocalDateTime.now());
            escalationRuleRepository.save(rule);
            return;
        }
        String title   = "🔴 URGENT — Doctor Escalation Required";
        String message = "URGENT: Patient has not responded and caregiver has been notified. " + rule.getContextMessage();
        deliveryService.deliverAll(
                rule.getPatientUserId(), rule.getReferenceId(),
                NotificationType.DOCTOR_ESCALATION, "CRITICAL",
                title, message,
                rule.getDoctorEmail(), rule.getDoctorPhone(),
                2, "esc-" + rule.getId()
        );
        rule.setCurrentLevel(2);
        rule.setLastEscalatedAt(LocalDateTime.now());
        rule.setActive(false); // escalation chain complete
        rule.setResolvedAt(LocalDateTime.now());
        escalationRuleRepository.save(rule);
        log.info("[Escalation] Level 2 (doctor) fired for patient {} — chain complete", rule.getPatientUserId());
    }

    /**
     * Mark an escalation as resolved (e.g. patient confirmed medication taken, checked in).
     */
    @Transactional
    public void resolveEscalation(Long patientUserId, EscalationType type, Long referenceId) {
        escalationRuleRepository
                .findByPatientUserIdAndEscalationTypeAndReferenceIdAndActiveTrue(
                        patientUserId, type, referenceId)
                .ifPresent(rule -> {
                    rule.setActive(false);
                    rule.setResolvedAt(LocalDateTime.now());
                    escalationRuleRepository.save(rule);
                    log.info("[Escalation] Resolved for patient {} type {} ref {}", patientUserId, type, referenceId);
                });
    }
}
