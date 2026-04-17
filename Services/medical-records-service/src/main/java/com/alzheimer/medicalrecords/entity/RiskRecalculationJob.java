package com.alzheimer.medicalrecords.entity;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;
import com.alzheimer.medicalrecords.service.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import com.alzheimer.medicalrecords.repository.MedicalRecordRepository;
import com.alzheimer.medicalrecords.service.RiskScoreService;

@Component
public class RiskRecalculationJob {

    private static final Logger log = LoggerFactory.getLogger(RiskRecalculationJob.class);

    private final MedicalRecordRepository medicalRecordRepository;
    private final RiskScoreService        riskScoreService;

    public RiskRecalculationJob(MedicalRecordRepository medicalRecordRepository,
                                 RiskScoreService riskScoreService) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.riskScoreService        = riskScoreService;
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void recalculateAllRiskScores() {
        log.info("=== Nightly risk recalculation started ===");
        long start = System.currentTimeMillis();
        List<MedicalRecord> all = medicalRecordRepository.findAllWithUser();
        int processed = 0, failed = 0;
        for (MedicalRecord record : all) {
            try {
                riskScoreService.updateRiskScore(record, "SCHEDULED_NIGHTLY_RECALCULATION");
                processed++;
            } catch (Exception e) {
                log.error("Failed to recalculate id={}", record.getId(), e);
                failed++;
            }
        }
        log.info("=== Nightly recalculation done: processed={}, failed={}, ms={} ===",
                processed, failed, System.currentTimeMillis() - start);
    }
}
