package com.alzheimer.medicalrecords.service;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RiskScoreService {

    private static final Logger log = LoggerFactory.getLogger(RiskScoreService.class);

    private final RiskFactorRepository riskFactorRepository;
    private final RiskScoreHistoryRepository historyRepository;

    public RiskScoreService(RiskFactorRepository riskFactorRepository,
                             RiskScoreHistoryRepository historyRepository) {
        this.riskFactorRepository = riskFactorRepository;
        this.historyRepository    = historyRepository;
    }

    @Transactional(readOnly = true)
    public double calculateRiskScore(MedicalRecord record) {
        double score = 0;
        score += ageFactor(record.getAge());
        score += familyHistoryFactor(record.getFamilyHistory());
        score += genderFactor(record.getGender(), record.getAge());
        score += educationFactor(record.getEducationLevel());   // String-based, same as original
        score += apoeFactor(record.getApoeStatus());            // NEW
        if (record.getId() != null) {
            score += clinicalRiskFactors(riskFactorRepository.findByMedicalRecordId(record.getId()));
        }
        score += cognitiveSymptoms(record.getCurrentSymptoms());
        score += hereditaryFactor(record.getHereditaryRiskContribution());
        score += wellnessFactor(record.getWellnessRiskContribution());
        score += adherenceFactor(record.getAdherenceRiskContribution());
        return Math.round(Math.max(0, Math.min(99, score)) * 10.0) / 10.0;
    }

    public RiskLevel determineRiskLevel(double score) {
        if (score < 20) return RiskLevel.LOW;
        if (score < 45) return RiskLevel.MEDIUM;
        if (score < 75) return RiskLevel.HIGH;   // FIX: was 70; HIGH = 45–74, CRITICAL = 75+
        return RiskLevel.CRITICAL;
    }

    @Transactional
    public void updateRiskScore(MedicalRecord record, String triggerReason) {
        double score    = calculateRiskScore(record);
        RiskLevel level = determineRiskLevel(score);
        record.setRiskScore(score);
        record.setRiskLevel(level);
        record.setLastRiskCalculation(LocalDateTime.now());
        // Auto-suggest diagnosis stage unless manually overridden
        if (!Boolean.TRUE.equals(record.getStageManuallyOverridden())) {
            record.setDiagnosisStage(DiagnosisStage.fromRiskScore(score));
        }
        if (record.getId() != null) {
            RiskScoreHistory snap = new RiskScoreHistory(
                    record.getId(), score, level,
                    record.getHereditaryRiskContribution(),
                    record.getWellnessRiskContribution(),
                    triggerReason);
            historyRepository.save(snap);
        }
    }

    @Transactional
    public void updateRiskScore(MedicalRecord record) {
        updateRiskScore(record, "MANUAL_UPDATE");
    }
    public void recalculateOnly(MedicalRecord record) {
        double score = calculateRiskScore(record);
        record.setRiskScore(score);
        record.setRiskLevel(determineRiskLevel(score));
        record.setLastRiskCalculation(java.time.LocalDateTime.now());
        if (!Boolean.TRUE.equals(record.getStageManuallyOverridden())) {
            record.setDiagnosisStage(DiagnosisStage.fromRiskScore(score));
        }
    }

    @Transactional(readOnly = true)
    public List<RiskScoreHistory> getHistory(Long recordId) {
        return historyRepository.findByRecordIdOrderByCalculatedAtDesc(recordId);
    }

    public Map<String, Double> getScoreBreakdown(MedicalRecord record) {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("age",             ageFactor(record.getAge()));
        m.put("familyHistory",   familyHistoryFactor(record.getFamilyHistory()));
        m.put("gender",          genderFactor(record.getGender(), record.getAge()));
        m.put("education",       educationFactor(record.getEducationLevel()));
        m.put("apoeGene",        apoeFactor(record.getApoeStatus()));
        List<RiskFactor> rf = record.getId() != null
                ? riskFactorRepository.findByMedicalRecordId(record.getId())
                : Collections.emptyList();
        m.put("clinicalFactors", clinicalRiskFactors(rf));
        m.put("symptoms",        cognitiveSymptoms(record.getCurrentSymptoms()));
        m.put("hereditary",      hereditaryFactor(record.getHereditaryRiskContribution()));
        m.put("wellness",        wellnessFactor(record.getWellnessRiskContribution()));
        double total = m.values().stream().mapToDouble(Double::doubleValue).sum();
        m.put("total", Math.round(Math.max(0, Math.min(99, total)) * 10.0) / 10.0);
        return m;
    }

    // ── Factors ───────────────────────────────────────────────────────────────

    private double ageFactor(Integer age) {
        if (age == null) return 0;
        if (age < 50)    return 0;
        if (age < 55)    return 4;
        if (age < 60)    return 8;
        if (age < 65)    return 13;
        if (age < 70)    return 18;
        if (age < 75)    return 23;
        if (age < 80)    return 27;
        return 30;
    }

    private double familyHistoryFactor(FamilyHistory fh) {
        return fh == FamilyHistory.Yes ? 20 : 0;
    }

    private double genderFactor(Gender gender, Integer age) {
        if (gender == Gender.Female && age != null && age >= 65) return 5;
        if (gender == Gender.Male   && age != null && age >= 70) return 3;
        return 0;
    }

    // educationLevel is a String (same as original) - matched by keyword
    private double educationFactor(String educationLevel) {
        if (educationLevel == null || educationLevel.isBlank()) return 0;
        String e = educationLevel.toLowerCase();
        if (e.contains("doctorate") || e.contains("phd") || e.contains("master")) return -10;
        if (e.contains("bachelor"))  return -6;
        if (e.contains("secondary") || e.contains("high school") || e.contains("baccalaureate") || e.contains("vocational")) return -3;
        return 0;
    }

    // NEW: APOE gene factor
    private double apoeFactor(APOEStatus status) {
        if (status == null || status == APOEStatus.NOT_TESTED) return 0;
        return status.getRiskContribution();
    }

    private double clinicalRiskFactors(List<RiskFactor> factors) {
        if (factors == null || factors.isEmpty()) return 0;
        double score = 0;
        for (RiskFactor rf : factors) {
            if (!Boolean.TRUE.equals(rf.getIsActive())) continue;
            double pts = switch (rf.getSeverity()) {
                case LOW      -> 2.0;
                case MEDIUM   -> 4.0;
                case HIGH     -> 6.0;
                case CRITICAL -> 9.0;
            };
            String type = rf.getFactorType() != null ? rf.getFactorType().toLowerCase() : "";
            if      (type.contains("stroke"))          pts *= 1.5;
            else if (type.contains("diabetes"))        pts *= 1.4;
            else if (type.contains("head trauma") || type.contains("tbi")) pts *= 1.4;
            else if (type.contains("cardiovascular"))  pts *= 1.35;
            else if (type.contains("hypertension"))    pts *= 1.3;
            else if (type.contains("depression"))      pts *= 1.25;
            else if (type.contains("sleep apnea"))     pts *= 1.2;
            score += pts;
        }
        return Math.min(25, score);
    }

    // cognitiveSymptoms takes String (same as original)
    private double cognitiveSymptoms(String symptoms) {
        if (symptoms == null || symptoms.isBlank()) return 0;
        String s = symptoms.toLowerCase();
        double score = 4;
        String[] high = {"memory loss","memory impairment","forgetting","confusion","disorientation",
                "getting lost","difficulty speaking","word finding","aphasia","personality change","behavioural change"};
        String[] med  = {"poor judgment","decision making","withdrawal","social isolation","mood changes",
                "depression","anxiety","difficulty concentrating","attention"};
        for (String sym : high) if (s.contains(sym)) score += 2.5;
        for (String sym : med)  if (s.contains(sym)) score += 1.5;
        return Math.min(20, score);
    }

    private double hereditaryFactor(Double v) {
        if (v == null || v <= 0) return 0;
        return Math.min(20, v * 0.20);
    }

    private double wellnessFactor(Double v) {
        if (v == null || v <= 0) return 0;
        return Math.min(20, v * 0.20);
    }

    /**
     * Poor medication adherence increases risk.
     * adherenceRiskContribution is set by medication-adherence-service:
     *   score = (100 - weeklyAdherenceScore) * 0.15 → max 15 points penalty
     */
    private double adherenceFactor(Double v) {
        if (v == null || v <= 0) return 0;
        return Math.min(15, v);
    }
}
