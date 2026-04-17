package com.alzheimer.medicalrecords;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;
import com.alzheimer.medicalrecords.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskScoreService Unit Tests")
class RiskScoreServiceTest {

    @Mock private RiskFactorRepository riskFactorRepository;
    @Mock private RiskScoreHistoryRepository riskScoreHistoryRepository;

    private RiskScoreService service;

    @BeforeEach
    void setUp() {
        service = new RiskScoreService(riskFactorRepository, riskScoreHistoryRepository);
        when(riskFactorRepository.findByMedicalRecordId(anyLong())).thenReturn(Collections.emptyList());
    }

    @Test @DisplayName("Age below 50 = 0 pts")
    void age_below50_zero() {
        assertThat(service.calculateRiskScore(record(45, Gender.Male, FamilyHistory.No, null))).isEqualTo(0.0);
    }

    @Test @DisplayName("Age 80+ = 30 pts (max)")
    void age_80plus_max() {
        assertThat(service.calculateRiskScore(record(85, Gender.Male, FamilyHistory.No, null))).isEqualTo(30.0);
    }

    @Test @DisplayName("Family history YES = +20 pts")
    void familyHistory_yes_20pts() {
        assertThat(service.calculateRiskScore(record(40, Gender.Male, FamilyHistory.Yes, null))).isEqualTo(20.0);
    }

    @Test @DisplayName("Doctorate education = -10 pts protective")
    void education_doctorate_protective() {
        assertThat(service.calculateRiskScore(record(70, Gender.Male, FamilyHistory.No, "Doctorate"))).isEqualTo(8.0);
    }

    @Test @DisplayName("Bachelor education = -6 pts protective")
    void education_bachelor_protective() {
        assertThat(service.calculateRiskScore(record(70, Gender.Male, FamilyHistory.No, "Bachelor"))).isEqualTo(12.0);
    }

    @Test @DisplayName("APOE E4/E4 homozygous = +25 pts")
    void apoe_e4e4_25pts() {
        MedicalRecord r = record(40, Gender.Male, FamilyHistory.No, null);
        r.setApoeStatus(APOEStatus.E4_E4);
        assertThat(service.calculateRiskScore(r)).isEqualTo(25.0);
    }

    @Test @DisplayName("APOE E3/E4 = +15 pts")
    void apoe_e3e4_15pts() {
        MedicalRecord r = record(40, Gender.Male, FamilyHistory.No, null);
        r.setApoeStatus(APOEStatus.E3_E4);
        assertThat(service.calculateRiskScore(r)).isEqualTo(15.0);
    }

    @Test @DisplayName("APOE E2/E3 = -4 pts protective")
    void apoe_e2e3_protective() {
        MedicalRecord r = record(70, Gender.Male, FamilyHistory.No, null);
        r.setApoeStatus(APOEStatus.E2_E3);
        assertThat(service.calculateRiskScore(r)).isEqualTo(14.0);
    }

    @Test @DisplayName("APOE NOT_TESTED = 0 pts")
    void apoe_notTested_zero() {
        MedicalRecord r = record(40, Gender.Male, FamilyHistory.No, null);
        r.setApoeStatus(APOEStatus.NOT_TESTED);
        assertThat(service.calculateRiskScore(r)).isEqualTo(0.0);
    }

    @Test @DisplayName("Symptoms string increases score")
    void symptoms_string_increase() {
        MedicalRecord r = record(40, Gender.Male, FamilyHistory.No, null);
        r.setCurrentSymptoms("memory loss and confusion");
        assertThat(service.calculateRiskScore(r)).isEqualTo(9.0);
    }

    @Test @DisplayName("Score < 20 = LOW")
    void level_low() { assertThat(service.determineRiskLevel(15.0)).isEqualTo(RiskLevel.LOW); }

    @Test @DisplayName("Score 20-44 = MEDIUM")
    void level_medium() { assertThat(service.determineRiskLevel(30.0)).isEqualTo(RiskLevel.MEDIUM); }

    @Test @DisplayName("Score 45-69 = HIGH")
    void level_high() { assertThat(service.determineRiskLevel(55.0)).isEqualTo(RiskLevel.HIGH); }

    @Test @DisplayName("Score 70+ = CRITICAL")
    void level_critical() { assertThat(service.determineRiskLevel(75.0)).isEqualTo(RiskLevel.CRITICAL); }

    @Test @DisplayName("Score always capped at 99")
    void score_cappedAt99() {
        MedicalRecord r = record(85, Gender.Female, FamilyHistory.Yes, null);
        r.setApoeStatus(APOEStatus.E4_E4);
        r.setCurrentSymptoms("memory loss confusion getting lost word finding aphasia personality change");
        r.setHereditaryRiskContribution(100.0);
        r.setWellnessRiskContribution(100.0);
        assertThat(service.calculateRiskScore(r)).isLessThanOrEqualTo(99.0);
    }

    @Test @DisplayName("Score never negative")
    void score_neverNegative() {
        MedicalRecord r = record(25, Gender.Male, FamilyHistory.No, "Doctorate");
        r.setApoeStatus(APOEStatus.E2_E2);
        assertThat(service.calculateRiskScore(r)).isGreaterThanOrEqualTo(0.0);
    }

    @Test @DisplayName("Breakdown has all factor keys")
    void breakdown_allKeys() {
        MedicalRecord r = record(60, Gender.Female, FamilyHistory.Yes, "Master");
        r.setId(1L);
        r.setApoeStatus(APOEStatus.E3_E4);
        Map<String, Double> breakdown = service.getScoreBreakdown(r);
        assertThat(breakdown).containsKeys("age","familyHistory","gender","education","apoeGene",
                "clinicalFactors","symptoms","hereditary","wellness","total");
    }

    @Test @DisplayName("DiagnosisStage.fromRiskScore: <20 = PRECLINICAL")
    void stage_preclinical() { assertThat(DiagnosisStage.fromRiskScore(10.0)).isEqualTo(DiagnosisStage.PRECLINICAL); }

    @Test @DisplayName("DiagnosisStage.fromRiskScore: 80+ = SEVERE")
    void stage_severe() { assertThat(DiagnosisStage.fromRiskScore(85.0)).isEqualTo(DiagnosisStage.SEVERE); }

    private MedicalRecord record(int age, Gender gender, FamilyHistory fh, String educationLevel) {
        MedicalRecord r = new MedicalRecord();
        r.setAge(age);
        r.setGender(gender);
        r.setFamilyHistory(fh);
        r.setEducationLevel(educationLevel);
        r.setApoeStatus(APOEStatus.NOT_TESTED);
        r.setHereditaryRiskContribution(0.0);
        r.setWellnessRiskContribution(0.0);
        return r;
    }
}
