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

/**
 * Listens for RiskUpdateEvent messages published by family-tree-service.
 *
 * When a user adds/updates/deletes a family member, family-tree-service
 * publishes a RiskUpdateEvent to RabbitMQ. This consumer picks it up
 * asynchronously and updates the user's medical record's hereditaryRiskContribution
 * and recalculates the overall risk score — without blocking the original request.
 */
@Component
public class RiskUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(RiskUpdateConsumer.class);

    private final MedicalRecordRepository medicalRecordRepository;
    private final RiskScoreService        riskScoreService;

    public RiskUpdateConsumer(MedicalRecordRepository medicalRecordRepository,
                              RiskScoreService riskScoreService) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.riskScoreService        = riskScoreService;
    }

    @RabbitListener(queues = RabbitMQConfig.FAMILY_QUEUE)
    @Transactional
    public void handleRiskUpdate(RiskUpdateEvent event) {
        if (event == null || event.getUserId() == null) {
            log.warn("[RabbitMQ] Received null or incomplete RiskUpdateEvent — skipping");
            return;
        }

        Long   userId = event.getUserId();
        double score  = event.getHereditaryRiskScore();
        log.info("[RabbitMQ] Received risk update for userId={} score={}", userId, score);

        try {
            List<MedicalRecord> records = medicalRecordRepository.findByUserIdWithUser(userId);
            if (records.isEmpty()) {
                log.debug("[RabbitMQ] No medical record for userId={} — skipping", userId);
                return;
            }

            MedicalRecord record = records.get(0);
            record.setHereditaryRiskContribution(score);
            riskScoreService.updateRiskScore(record, "FAMILY_TREE_UPDATED");
            medicalRecordRepository.save(record);

            log.info("[RabbitMQ] Updated hereditary risk for userId={} recordId={} newScore={}",
                    userId, record.getId(), record.getRiskScore());

        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to process risk update for userId={}: {}",
                    userId, e.getMessage(), e);
            // Re-throw to trigger RabbitMQ retry / dead-letter if configured
            throw new RuntimeException("Risk update processing failed for userId=" + userId, e);
        }
    }
}
