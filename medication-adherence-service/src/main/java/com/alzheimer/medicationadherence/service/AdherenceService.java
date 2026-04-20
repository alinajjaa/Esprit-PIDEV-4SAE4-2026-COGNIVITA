package com.alzheimer.medicationadherence.service;

import com.alzheimer.medicationadherence.dto.*;
import com.alzheimer.medicationadherence.entity.*;
import com.alzheimer.medicationadherence.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AdherenceService {

    private static final Logger log = LoggerFactory.getLogger(AdherenceService.class);

    // Known drug interaction pairs (surfaces MedicationInteractionService logic locally)
    private static final List<DrugInteraction> INTERACTIONS = List.of(
        new DrugInteraction("donepezil", "amitriptyline", "HIGH",
            "Cholinesterase inhibitor + tricyclic antidepressant: additive anticholinergic effects may worsen cognition.",
            "Consider switching amitriptyline to an SSRI."),
        new DrugInteraction("rivastigmine", "oxybutynin", "HIGH",
            "Cholinesterase inhibitor + anticholinergic bladder drug: opposing mechanisms reduce effectiveness.",
            "Consider mirabegron (non-anticholinergic) for bladder symptoms."),
        new DrugInteraction("galantamine", "diphenhydramine", "HIGH",
            "Cholinesterase inhibitor + Benadryl: anticholinergic antagonism reduces dementia drug efficacy.",
            "Avoid diphenhydramine entirely. Use non-sedating antihistamines."),
        new DrugInteraction("diazepam", "lorazepam", "HIGH",
            "Two benzodiazepines co-prescribed: cumulative CNS depression, fall and confusion risk.",
            "Use only one benzodiazepine at the lowest effective dose."),
        new DrugInteraction("haloperidol", "quetiapine", "HIGH",
            "Two antipsychotics co-prescribed: increased QT prolongation and metabolic risk.",
            "Monotherapy preferred. Consult psychiatry."),
        new DrugInteraction("amitriptyline", "diphenhydramine", "MEDIUM",
            "Combined anticholinergic burden increases dementia risk and causes confusion.",
            "Eliminate diphenhydramine. SSRIs preferred over amitriptyline."),
        new DrugInteraction("memantine", "ketamine", "MEDIUM",
            "Both are NMDA antagonists: additive CNS effects may cause confusion.",
            "Avoid concurrent use. Consult psychiatry if ketamine infusions are planned.")
    );

    // Single-drug cognitive risk flags
    private static final Map<String, SingleDrugFlag> RISK_FLAGS = new LinkedHashMap<>();
    static {
        RISK_FLAGS.put("diphenhydramine", new SingleDrugFlag("HIGH",
            "Strong anticholinergic — linked to 44% increased dementia risk.", "Replace with loratadine or cetirizine."));
        RISK_FLAGS.put("benadryl", new SingleDrugFlag("HIGH",
            "Diphenhydramine (Benadryl) — blocks acetylcholine critical for memory.", "Avoid in patients 65+."));
        RISK_FLAGS.put("diazepam", new SingleDrugFlag("HIGH",
            "Benzodiazepine — 50% increased Alzheimer's risk with long-term use.", "Consider SSRI for anxiety."));
        RISK_FLAGS.put("lorazepam", new SingleDrugFlag("HIGH",
            "Benzodiazepine — sedation, memory impairment, fall risk.", "Use short-term only."));
        RISK_FLAGS.put("zolpidem", new SingleDrugFlag("MEDIUM",
            "Z-drug — associated with cognitive decline and falls.", "CBT-I is first-line for insomnia."));
        RISK_FLAGS.put("amitriptyline", new SingleDrugFlag("HIGH",
            "Tricyclic with strong anticholinergic load — confirmed dementia risk.", "SSRIs are preferred."));
        RISK_FLAGS.put("haloperidol", new SingleDrugFlag("HIGH",
            "Antipsychotic — increased mortality in elderly dementia patients.", "Use lowest effective dose."));
        RISK_FLAGS.put("quetiapine", new SingleDrugFlag("MEDIUM",
            "Antipsychotic — metabolic effects, sedation risk.", "Monitor quarterly."));
        RISK_FLAGS.put("donepezil", new SingleDrugFlag("PROTECTIVE",
            "Cholinesterase inhibitor — protective against cognitive decline.", "Continue as prescribed."));
        RISK_FLAGS.put("memantine", new SingleDrugFlag("PROTECTIVE",
            "NMDA receptor antagonist — reduces glutamate excitotoxicity.", "Continue as prescribed."));
    }

    private final MedicationLogRepository medicationLogRepository;
    private final AdherenceScoreRepository adherenceScoreRepository;

    public AdherenceService(MedicationLogRepository medicationLogRepository,
                             AdherenceScoreRepository adherenceScoreRepository) {
        this.medicationLogRepository  = medicationLogRepository;
        this.adherenceScoreRepository = adherenceScoreRepository;
    }

    // ── Check-in (patient confirms dose taken) ───────────────────────────────

    @Transactional
    public MedicationLog confirmDoseTaken(Long patientUserId, Long medicationId,
                                           LocalDate date, String notes) {
        MedicationLog log2 = medicationLogRepository
            .findByPatientUserIdAndMedicationIdAndScheduledDate(patientUserId, medicationId, date)
            .orElseThrow(() -> new NoSuchElementException(
                "No scheduled dose found for medication " + medicationId + " on " + date));

        if (log2.getStatus() == DoseStatus.TAKEN) {
            return log2; // idempotent
        }
        log2.setStatus(DoseStatus.TAKEN);
        log2.setTakenAt(LocalDateTime.now());
        log2.setPatientNotes(notes);
        MedicationLog saved = medicationLogRepository.save(log2);

        log.info("[AdherenceService] Patient {} confirmed dose of {} on {}",
                patientUserId, log2.getMedicationName(), date);

        // Recalculate and publish updated score
        recalculateAndPublish(patientUserId);
        return saved;
    }

    @Transactional
    public MedicationLog skipDose(Long patientUserId, Long medicationId,
                                   LocalDate date, String reason) {
        MedicationLog entry = medicationLogRepository
            .findByPatientUserIdAndMedicationIdAndScheduledDate(patientUserId, medicationId, date)
            .orElseThrow(() -> new NoSuchElementException("No scheduled dose found"));
        entry.setStatus(DoseStatus.SKIPPED);
        entry.setPatientNotes(reason);
        MedicationLog saved = medicationLogRepository.save(entry);
        recalculateAndPublish(patientUserId);
        return saved;
    }

    // ── Schedule a dose log (called when medication is added/updated) ─────────

    @Transactional
    public MedicationLog scheduleDose(Long patientUserId, Long medicationId,
                                       String medicationName, String dosage, String frequency,
                                       LocalDateTime scheduledTime,
                                       String patientName, String patientEmail, String patientPhone,
                                       String caregiverEmail, String caregiverPhone,
                                       String doctorEmail, String doctorPhone) {
        LocalDate date = scheduledTime.toLocalDate();
        // Skip if already exists
        Optional<MedicationLog> existing = medicationLogRepository
            .findByPatientUserIdAndMedicationIdAndScheduledDate(patientUserId, medicationId, date);
        if (existing.isPresent()) return existing.get();

        MedicationLog entry = new MedicationLog();
        entry.setPatientUserId(patientUserId);
        entry.setMedicationId(medicationId);
        entry.setMedicationName(medicationName);
        entry.setDosage(dosage);
        entry.setFrequency(frequency);
        entry.setScheduledTime(scheduledTime);
        entry.setScheduledDate(date);
        entry.setStatus(DoseStatus.PENDING);
        entry.setPatientName(patientName);
        entry.setPatientEmail(patientEmail);
        entry.setPatientPhone(patientPhone);
        entry.setCaregiverEmail(caregiverEmail);
        entry.setCaregiverPhone(caregiverPhone);
        entry.setDoctorEmail(doctorEmail);
        entry.setDoctorPhone(doctorPhone);
        return medicationLogRepository.save(entry);
    }

    // ── Adherence Score Calculation ──────────────────────────────────────────

    @Transactional
    public AdherenceScore recalculateAndPublish(Long patientUserId) {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo  = today.minusDays(7);
        LocalDate thirtyDaysAgo = today.minusDays(30);

        List<MedicationLog> weeklyLogs  = medicationLogRepository
            .findByPatientAndDateRange(patientUserId, sevenDaysAgo, today);
        List<MedicationLog> monthlyLogs = medicationLogRepository
            .findByPatientAndDateRange(patientUserId, thirtyDaysAgo, today);

        double weeklyScore  = computeAdherencePercent(weeklyLogs);
        double monthlyScore = computeAdherencePercent(monthlyLogs);
        int streak          = computeStreak(patientUserId, today);

        long totalScheduled = medicationLogRepository.countByPatientUserIdAndStatus(patientUserId, DoseStatus.PENDING)
            + medicationLogRepository.countByPatientUserIdAndStatus(patientUserId, DoseStatus.TAKEN)
            + medicationLogRepository.countByPatientUserIdAndStatus(patientUserId, DoseStatus.MISSED)
            + medicationLogRepository.countByPatientUserIdAndStatus(patientUserId, DoseStatus.SKIPPED);
        long totalTaken  = medicationLogRepository.countByPatientUserIdAndStatus(patientUserId, DoseStatus.TAKEN);
        long totalMissed = medicationLogRepository.countByPatientUserIdAndStatus(patientUserId, DoseStatus.MISSED);

        AdherenceScore score = adherenceScoreRepository.findByPatientUserId(patientUserId)
            .orElseGet(() -> { AdherenceScore s = new AdherenceScore(); s.setPatientUserId(patientUserId); return s; });
        score.setWeeklyScore(weeklyScore);
        score.setMonthlyScore(monthlyScore);
        score.setStreakDays(streak);
        score.setTotalDosesScheduled((int) totalScheduled);
        score.setTotalDosesTaken((int) totalTaken);
        score.setTotalDosesMissed((int) totalMissed);
        score.setLastCalculated(LocalDateTime.now());
        AdherenceScore saved = adherenceScoreRepository.save(score);
        return saved;
    }

    private double computeAdherencePercent(List<MedicationLog> logs) {
        if (logs.isEmpty()) return 100.0;
        long completed = logs.stream()
            .filter(l -> l.getStatus() == DoseStatus.TAKEN || l.getStatus() == DoseStatus.SKIPPED)
            .count();
        long total = logs.stream()
            .filter(l -> l.getStatus() != DoseStatus.PENDING)
            .count();
        if (total == 0) return 100.0;
        return Math.round((completed * 100.0 / total) * 10.0) / 10.0;
    }

    private int computeStreak(Long patientUserId, LocalDate today) {
        List<LocalDate> dates = medicationLogRepository
            .findDistinctDates(patientUserId, today.minusDays(90));
        int streak = 0;
        LocalDate expected = today;
        for (LocalDate date : dates) {
            if (!date.equals(expected)) break;
            // Check all doses for this day were taken/skipped
            List<MedicationLog> dayLogs = medicationLogRepository
                .findByPatientUserIdAndScheduledDateOrderByScheduledTimeAsc(patientUserId, date);
            boolean allDone = dayLogs.stream()
                .allMatch(l -> l.getStatus() == DoseStatus.TAKEN || l.getStatus() == DoseStatus.SKIPPED);
            if (!allDone) break;
            streak++;
            expected = expected.minusDays(1);
        }
        return streak;
    }

    // ── Drug Interaction Checking ────────────────────────────────────────────

    public DrugInteractionReport checkInteractions(List<String> medicationNames) {
        List<String> normalized = medicationNames.stream()
            .map(String::toLowerCase).map(String::trim).toList();

        List<InteractionAlert> alerts = new ArrayList<>();

        // Check pairs
        for (DrugInteraction pair : INTERACTIONS) {
            boolean hasDrug1 = normalized.stream().anyMatch(n -> n.contains(pair.drug1()));
            boolean hasDrug2 = normalized.stream().anyMatch(n -> n.contains(pair.drug2()));
            if (hasDrug1 && hasDrug2) {
                alerts.add(new InteractionAlert("INTERACTION", pair.severity(),
                    pair.drug1(), pair.drug2(), pair.message(), pair.recommendation()));
            }
        }

        // Check single-drug flags
        for (String name : normalized) {
            for (Map.Entry<String, SingleDrugFlag> entry : RISK_FLAGS.entrySet()) {
                if (name.contains(entry.getKey())) {
                    SingleDrugFlag flag = entry.getValue();
                    if (!"PROTECTIVE".equals(flag.severity())) {
                        alerts.add(new InteractionAlert("RISK_FLAG", flag.severity(),
                            entry.getKey(), null, flag.message(), flag.recommendation()));
                    } else {
                        alerts.add(new InteractionAlert("PROTECTIVE", "INFO",
                            entry.getKey(), null, flag.message(), flag.recommendation()));
                    }
                }
            }
        }

        String overallRisk = alerts.stream().anyMatch(a -> "HIGH".equals(a.severity())) ? "HIGH"
            : alerts.stream().anyMatch(a -> "MEDIUM".equals(a.severity())) ? "MEDIUM" : "LOW";

        return new DrugInteractionReport(medicationNames, alerts, overallRisk);
    }

    // ── Query methods ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MedicationLog> getTodaysLogs(Long patientUserId) {
        return medicationLogRepository
            .findByPatientUserIdAndScheduledDateOrderByScheduledTimeAsc(patientUserId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<MedicationLog> getLogsForDate(Long patientUserId, LocalDate date) {
        return medicationLogRepository
            .findByPatientUserIdAndScheduledDateOrderByScheduledTimeAsc(patientUserId, date);
    }

    @Transactional(readOnly = true)
    public Optional<AdherenceScore> getAdherenceScore(Long patientUserId) {
        return adherenceScoreRepository.findByPatientUserId(patientUserId);
    }

    @Transactional(readOnly = true)
    public List<MedicationLog> getHistory(Long patientUserId) {
        return medicationLogRepository.findByPatientUserIdOrderByScheduledTimeDesc(patientUserId);
    }

    // ── Inner records ────────────────────────────────────────────────────────
    private record DrugInteraction(String drug1, String drug2, String severity,
                                    String message, String recommendation) {}
    private record SingleDrugFlag(String severity, String message, String recommendation) {}
    public record InteractionAlert(String type, String severity, String drug1, String drug2,
                                    String message, String recommendation) {}
    public record DrugInteractionReport(List<String> checkedMedications,
                                         List<InteractionAlert> alerts, String overallRisk) {}
}
