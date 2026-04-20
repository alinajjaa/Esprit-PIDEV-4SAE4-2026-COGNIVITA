package com.alzheimer.medicationadherence.service;

import com.alzheimer.medicationadherence.entity.AdherenceScore;
import com.alzheimer.medicationadherence.entity.DoseStatus;
import com.alzheimer.medicationadherence.entity.MedicationLog;
import com.alzheimer.medicationadherence.repository.AdherenceScoreRepository;
import com.alzheimer.medicationadherence.repository.MedicationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdherenceService Unit Tests")
class AdherenceServiceTest {

    @Mock
    private MedicationLogRepository medicationLogRepository;

    @Mock
    private AdherenceScoreRepository adherenceScoreRepository;

    @InjectMocks
    private AdherenceService service;

    private MedicationLog pendingLog;

    @BeforeEach
    void setUp() {
        pendingLog = new MedicationLog();
        pendingLog.setPatientUserId(1L);
        pendingLog.setMedicationId(10L);
        pendingLog.setMedicationName("donepezil");
        pendingLog.setScheduledDate(LocalDate.now());
        pendingLog.setStatus(DoseStatus.PENDING);
    }

    // ── confirmDoseTaken ──────────────────────────────────────────────────────

    @Test
    @DisplayName("confirmDoseTaken: marks dose as TAKEN and recalculates score")
    void confirmDoseTaken_marksTakenAndRecalculates() {
        when(medicationLogRepository.findByPatientUserIdAndMedicationIdAndScheduledDate(1L, 10L, LocalDate.now()))
                .thenReturn(Optional.of(pendingLog));
        when(medicationLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        stubRecalculate();

        MedicationLog result = service.confirmDoseTaken(1L, 10L, LocalDate.now(), "Taken with food");

        assertThat(result.getStatus()).isEqualTo(DoseStatus.TAKEN);
        assertThat(result.getPatientNotes()).isEqualTo("Taken with food");
        assertThat(result.getTakenAt()).isNotNull();
    }

    @Test
    @DisplayName("confirmDoseTaken: idempotent when dose already TAKEN")
    void confirmDoseTaken_idempotentWhenAlreadyTaken() {
        pendingLog.setStatus(DoseStatus.TAKEN);
        when(medicationLogRepository.findByPatientUserIdAndMedicationIdAndScheduledDate(any(), any(), any()))
                .thenReturn(Optional.of(pendingLog));

        MedicationLog result = service.confirmDoseTaken(1L, 10L, LocalDate.now(), null);

        assertThat(result.getStatus()).isEqualTo(DoseStatus.TAKEN);
        verify(medicationLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmDoseTaken: throws when no scheduled dose found")
    void confirmDoseTaken_whenNotFound_throws() {
        when(medicationLogRepository.findByPatientUserIdAndMedicationIdAndScheduledDate(any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmDoseTaken(1L, 10L, LocalDate.now(), null))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── skipDose ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("skipDose: marks dose as SKIPPED with reason")
    void skipDose_marksSkipped() {
        when(medicationLogRepository.findByPatientUserIdAndMedicationIdAndScheduledDate(any(), any(), any()))
                .thenReturn(Optional.of(pendingLog));
        when(medicationLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        stubRecalculate();

        MedicationLog result = service.skipDose(1L, 10L, LocalDate.now(), "Feeling sick");

        assertThat(result.getStatus()).isEqualTo(DoseStatus.SKIPPED);
        assertThat(result.getPatientNotes()).isEqualTo("Feeling sick");
    }

    // ── scheduleDose ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("scheduleDose: creates new PENDING log when none exists")
    void scheduleDose_createsNewLog() {
        when(medicationLogRepository.findByPatientUserIdAndMedicationIdAndScheduledDate(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(medicationLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        LocalDateTime time = LocalDateTime.now();
        MedicationLog result = service.scheduleDose(1L, 10L, "donepezil", "5mg", "Daily",
                time, "Alice", "alice@x.com", "+21612345678",
                "caregiver@x.com", "+21699999999",
                "doctor@x.com", "+21688888888");

        assertThat(result.getStatus()).isEqualTo(DoseStatus.PENDING);
        assertThat(result.getMedicationName()).isEqualTo("donepezil");
        verify(medicationLogRepository).save(any());
    }

    @Test
    @DisplayName("scheduleDose: returns existing log without saving when already scheduled")
    void scheduleDose_returnsExistingWhenPresent() {
        when(medicationLogRepository.findByPatientUserIdAndMedicationIdAndScheduledDate(any(), any(), any()))
                .thenReturn(Optional.of(pendingLog));

        MedicationLog result = service.scheduleDose(1L, 10L, "donepezil", "5mg", "Daily",
                LocalDateTime.now(), null, null, null, null, null, null, null);

        assertThat(result).isSameAs(pendingLog);
        verify(medicationLogRepository, never()).save(any());
    }

    // ── Drug Interaction Checking ─────────────────────────────────────────────

    @Test
    @DisplayName("checkInteractions: detects donepezil + amitriptyline HIGH interaction")
    void checkInteractions_detectsHighInteraction() {
        AdherenceService.DrugInteractionReport report =
                service.checkInteractions(List.of("Donepezil 10mg", "Amitriptyline 25mg"));

        assertThat(report.overallRisk()).isEqualTo("HIGH");
        assertThat(report.alerts()).isNotEmpty();
        assertThat(report.alerts().stream().anyMatch(a -> "INTERACTION".equals(a.type()))).isTrue();
    }

    @Test
    @DisplayName("checkInteractions: detects single drug flag for diazepam")
    void checkInteractions_detectsSingleDrugFlag_diazepam() {
        AdherenceService.DrugInteractionReport report =
                service.checkInteractions(List.of("diazepam 5mg"));

        assertThat(report.overallRisk()).isEqualTo("HIGH");
        assertThat(report.alerts().stream().anyMatch(a -> "RISK_FLAG".equals(a.type()))).isTrue();
    }

    @Test
    @DisplayName("checkInteractions: donepezil is flagged as PROTECTIVE")
    void checkInteractions_donepezilMarkedProtective() {
        AdherenceService.DrugInteractionReport report =
                service.checkInteractions(List.of("donepezil 5mg"));

        assertThat(report.overallRisk()).isEqualTo("LOW");
        assertThat(report.alerts().stream().anyMatch(a -> "PROTECTIVE".equals(a.type()))).isTrue();
    }

    @Test
    @DisplayName("checkInteractions: no interactions with safe medications")
    void checkInteractions_noAlertsForSafeMeds() {
        AdherenceService.DrugInteractionReport report =
                service.checkInteractions(List.of("paracetamol", "vitamin C"));

        assertThat(report.alerts()).isEmpty();
        assertThat(report.overallRisk()).isEqualTo("LOW");
    }

    // ── Query methods ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAdherenceScore: returns Optional from repository")
    void getAdherenceScore_returnsFromRepository() {
        AdherenceScore score = new AdherenceScore();
        score.setPatientUserId(1L);
        score.setWeeklyScore(90.0);

        when(adherenceScoreRepository.findByPatientUserId(1L)).thenReturn(Optional.of(score));

        Optional<AdherenceScore> result = service.getAdherenceScore(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getWeeklyScore()).isEqualTo(90.0);
    }

    @Test
    @DisplayName("getAdherenceScore: returns empty Optional when no score")
    void getAdherenceScore_emptyWhenNone() {
        when(adherenceScoreRepository.findByPatientUserId(99L)).thenReturn(Optional.empty());

        assertThat(service.getAdherenceScore(99L)).isEmpty();
    }

    @Test
    @DisplayName("getTodaysLogs: delegates to repository for today's date")
    void getTodaysLogs_delegatesToRepository() {
        when(medicationLogRepository.findByPatientUserIdAndScheduledDateOrderByScheduledTimeAsc(1L, LocalDate.now()))
                .thenReturn(Collections.singletonList(pendingLog));

        List<MedicationLog> result = service.getTodaysLogs(1L);

        assertThat(result).hasSize(1);
    }

    // ── Private helper to stub recalculateAndPublish deps ────────────────────

    private void stubRecalculate() {
        when(medicationLogRepository.findByPatientAndDateRange(eq(1L), any(), any()))
                .thenReturn(Collections.emptyList());
        when(medicationLogRepository.findDistinctDates(eq(1L), any()))
                .thenReturn(Collections.emptyList());
        when(medicationLogRepository.countByPatientUserIdAndStatus(anyLong(), any()))
                .thenReturn(0L);
        when(adherenceScoreRepository.findByPatientUserId(1L)).thenReturn(Optional.empty());
        when(adherenceScoreRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }
}
