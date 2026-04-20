package com.alzheimer.medicationadherence.scheduler;

import com.alzheimer.medicationadherence.entity.DoseStatus;
import com.alzheimer.medicationadherence.entity.MedicationLog;
import com.alzheimer.medicationadherence.repository.MedicationLogRepository;
import com.alzheimer.medicationadherence.service.AdherenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MissedDoseDetectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(MissedDoseDetectionScheduler.class);

    private final MedicationLogRepository medicationLogRepository;
    private final AdherenceService adherenceService;

    @Value("${adherence.missed-dose-threshold-hours:4}")
    private long missedDoseThresholdHours;

    public MissedDoseDetectionScheduler(MedicationLogRepository medicationLogRepository,
                                         AdherenceService adherenceService) {
        this.medicationLogRepository = medicationLogRepository;
        this.adherenceService        = adherenceService;
    }

    /** Every 4 hours: find PENDING doses past threshold and mark them MISSED. */
    @Scheduled(fixedDelayString = "PT4H")
    @Transactional
    public void detectMissedDoses() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(missedDoseThresholdHours);
        List<MedicationLog> overdue = medicationLogRepository.findOverduePendingLogs(threshold);

        if (overdue.isEmpty()) {
            log.debug("[MissedDoseDetection] No overdue doses found.");
            return;
        }

        log.info("[MissedDoseDetection] Found {} overdue dose(s) — marking MISSED and publishing events", overdue.size());

        Set<Long> affectedPatients = overdue.stream()
            .map(MedicationLog::getPatientUserId)
            .collect(Collectors.toSet());

        for (MedicationLog entry : overdue) {
            entry.setStatus(DoseStatus.MISSED);
            entry.setAlertSent(true);
            medicationLogRepository.save(entry);
        }

        // Recalculate adherence scores for all affected patients and publish to medical-records-service
        for (Long patientUserId : affectedPatients) {
            try {
                adherenceService.recalculateAndPublish(patientUserId);
            } catch (Exception e) {
                log.error("[MissedDoseDetection] Failed to recalculate score for patient {}: {}",
                        patientUserId, e.getMessage());
            }
        }
    }
}
