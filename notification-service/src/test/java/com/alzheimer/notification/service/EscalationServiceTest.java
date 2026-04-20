package com.alzheimer.notification.service;

import com.alzheimer.notification.entity.EscalationRule;
import com.alzheimer.notification.entity.EscalationType;
import com.alzheimer.notification.repository.EscalationRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EscalationService Unit Tests")
class EscalationServiceTest {

    @Mock
    private EscalationRuleRepository escalationRuleRepository;

    @Mock
    private MultiChannelDeliveryService deliveryService;

    @InjectMocks
    private EscalationService service;

    private EscalationRule rule;

    @BeforeEach
    void setUp() {
        rule = new EscalationRule();
        rule.setId(1L);
        rule.setPatientUserId(42L);
        rule.setEscalationType(EscalationType.MISSED_DOSE);
        rule.setReferenceId(10L);
        rule.setContextMessage("Patient missed donepezil.");
        rule.setCaregiverEmail("caregiver@x.com");
        rule.setCaregiverPhone("+21699999999");
        rule.setDoctorEmail("doctor@x.com");
        rule.setDoctorPhone("+21688888888");
        rule.setCurrentLevel(0);
        rule.setActive(true);
    }

    // ── createEscalation ─────────────────────────────────────────────────────

    @Test
    @DisplayName("createEscalation: creates new rule when none active")
    void createEscalation_createsNewRule() {
        when(escalationRuleRepository
                .findByPatientUserIdAndEscalationTypeAndReferenceIdAndActiveTrue(42L, EscalationType.MISSED_DOSE, 10L))
                .thenReturn(Optional.empty());
        when(escalationRuleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        EscalationRule result = service.createEscalation(
                42L, EscalationType.MISSED_DOSE, 10L, "Patient missed donepezil.",
                "caregiver@x.com", "+21699999999", "doctor@x.com", "+21688888888");

        assertThat(result.getCurrentLevel()).isEqualTo(0);
        assertThat(result.getActive()).isTrue();
        verify(escalationRuleRepository).save(any());
    }

    @Test
    @DisplayName("createEscalation: returns existing rule when one is already active")
    void createEscalation_idempotentWhenAlreadyActive() {
        when(escalationRuleRepository
                .findByPatientUserIdAndEscalationTypeAndReferenceIdAndActiveTrue(any(), any(), any()))
                .thenReturn(Optional.of(rule));

        EscalationRule result = service.createEscalation(
                42L, EscalationType.MISSED_DOSE, 10L, "msg",
                null, null, null, null);

        assertThat(result).isSameAs(rule);
        verify(escalationRuleRepository, never()).save(any());
    }

    // ── escalateToCaregiver ───────────────────────────────────────────────────

    @Test
    @DisplayName("escalateToCaregiver: sends to caregiver and sets level=1")
    void escalateToCaregiver_sendsAndSetsLevel1() {
        when(escalationRuleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.escalateToCaregiver(rule);

        assertThat(rule.getCurrentLevel()).isEqualTo(1);
        assertThat(rule.getLastEscalatedAt()).isNotNull();
        verify(deliveryService).deliverAll(
                eq(42L), eq(10L), any(), eq("WARNING"),
                anyString(), anyString(),
                eq("caregiver@x.com"), eq("+21699999999"),
                eq(1), anyString());
        verify(escalationRuleRepository).save(rule);
    }

    @Test
    @DisplayName("escalateToCaregiver: skips to doctor when no caregiver contact")
    void escalateToCaregiver_noCaregiverSkipsToDoctor() {
        rule.setCaregiverEmail(null);
        rule.setCaregiverPhone(null);
        when(escalationRuleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.escalateToCaregiver(rule);

        // Should have escalated to doctor instead
        assertThat(rule.getCurrentLevel()).isEqualTo(2);
        assertThat(rule.getActive()).isFalse();
    }

    // ── escalateToDoctor ──────────────────────────────────────────────────────

    @Test
    @DisplayName("escalateToDoctor: sends to doctor, sets level=2 and deactivates")
    void escalateToDoctor_sendsAndClosesChain() {
        when(escalationRuleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.escalateToDoctor(rule);

        assertThat(rule.getCurrentLevel()).isEqualTo(2);
        assertThat(rule.getActive()).isFalse();
        assertThat(rule.getResolvedAt()).isNotNull();
        verify(deliveryService).deliverAll(
                eq(42L), eq(10L), any(), eq("CRITICAL"),
                anyString(), anyString(),
                eq("doctor@x.com"), eq("+21688888888"),
                eq(2), anyString());
    }

    @Test
    @DisplayName("escalateToDoctor: deactivates without delivery when no doctor contact")
    void escalateToDoctor_noDoctorContact_deactivatesOnly() {
        rule.setDoctorEmail(null);
        rule.setDoctorPhone(null);
        when(escalationRuleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.escalateToDoctor(rule);

        assertThat(rule.getActive()).isFalse();
        verify(deliveryService, never()).deliverAll(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any());
    }

    // ── resolveEscalation ─────────────────────────────────────────────────────

    @Test
    @DisplayName("resolveEscalation: marks active rule as inactive")
    void resolveEscalation_marksInactive() {
        when(escalationRuleRepository
                .findByPatientUserIdAndEscalationTypeAndReferenceIdAndActiveTrue(42L, EscalationType.MISSED_DOSE, 10L))
                .thenReturn(Optional.of(rule));
        when(escalationRuleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.resolveEscalation(42L, EscalationType.MISSED_DOSE, 10L);

        assertThat(rule.getActive()).isFalse();
        assertThat(rule.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("resolveEscalation: does nothing when no active rule found")
    void resolveEscalation_doesNothingWhenNotFound() {
        when(escalationRuleRepository
                .findByPatientUserIdAndEscalationTypeAndReferenceIdAndActiveTrue(any(), any(), any()))
                .thenReturn(Optional.empty());

        service.resolveEscalation(99L, EscalationType.MISSED_DOSE, 99L);

        verify(escalationRuleRepository, never()).save(any());
    }
}
