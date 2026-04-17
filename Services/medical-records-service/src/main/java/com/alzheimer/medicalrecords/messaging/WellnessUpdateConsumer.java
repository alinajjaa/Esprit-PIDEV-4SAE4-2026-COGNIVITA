package com.alzheimer.medicalrecords.messaging;

import com.alzheimer.medicalrecords.entity.MedicalRecord;
import com.alzheimer.medicalrecords.repository.MedicalRecordRepository;
import com.alzheimer.medicalrecords.service.RiskScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Listens for WellnessUpdateEvent messages published by health-prevention-service.
 *
 * When a user creates or updates their health/wellness profile, the wellness risk
 * score is propagated here asynchronously and applied to the user's medical record.
 * This was previously silently broken — the BackendServiceClient Feign interface
 * was declared in health-prevention-service but never injected or called anywhere.
 */
@Component
public class WellnessUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(WellnessUpdateConsumer.class);

    private static final String WELLNESS_QUEUE = "health-prevention.wellness-updates";

    private final MedicalRecordRepository medicalRecordRepository;
    private final RiskScoreService        riskScoreService;

    public WellnessUpdateConsumer(MedicalRecordRepository medicalRecordRepository,
                                  RiskScoreService riskScoreService) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.riskScoreService        = riskScoreService;
    }

    @RabbitListener(queues = WELLNESS_QUEUE)
    @Transactional
    public void handleWellnessUpdate(WellnessUpdateEvent event) {
        if (event == null || event.getUserId() == null) {
            log.warn("[RabbitMQ] Received null or incomplete WellnessUpdateEvent — skipping");
            return;
        }

        Long   userId  = event.getUserId();
        double score   = event.getWellnessScore();
        log.info("[RabbitMQ] Received wellness update for userId={} score={}", userId, score);

        try {
            MedicalRecord record = null;

            // Prefer direct recordId lookup if provided
            if (event.getMedicalRecordId() != null) {
                Optional<MedicalRecord> opt = medicalRecordRepository.findByIdWithUser(event.getMedicalRecordId());
                if (opt.isPresent()) record = opt.get();
            }

            // Fallback: find by userId
            if (record == null) {
                List<MedicalRecord> records = medicalRecordRepository.findByUserIdWithUser(userId);
                if (records.isEmpty()) {
                    log.debug("[RabbitMQ] No medical record for userId={} — skipping wellness update", userId);
                    return;
                }
                record = records.get(0);
            }

            record.setWellnessRiskContribution(score);
            riskScoreService.updateRiskScore(record, "WELLNESS_PROFILE_UPDATED");
            medicalRecordRepository.save(record);

            log.info("[RabbitMQ] Updated wellness risk for userId={} recordId={} newRiskScore={}",
                    userId, record.getId(), record.getRiskScore());

        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to process wellness update for userId={}: {}",
                    userId, e.getMessage(), e);
            throw new RuntimeException("Wellness update processing failed for userId=" + userId, e);
        }
    }
}
