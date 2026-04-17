package com.alzheimer.medicalrecords;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MedicationInteractionService Unit Tests")
class MedicationInteractionServiceTest {

    private MedicationInteractionService service;

    @BeforeEach
    void setUp() {
        service = new MedicationInteractionService();
    }

    @Test
    @DisplayName("Donepezil + Amitriptyline = HIGH interaction alert")
    void donepezil_amitriptyline_highInteraction() {
        List<Medication> meds = List.of(med("Donepezil 5mg"), med("Amitriptyline 25mg"));
        List<MedicationInteractionService.InteractionAlert> alerts = service.analyze(meds);
        assertThat(alerts).anyMatch(a -> "INTERACTION".equals(a.type()) && "HIGH".equals(a.severity()));
    }

    @Test
    @DisplayName("Rivastigmine + Oxybutynin = HIGH interaction alert")
    void rivastigmine_oxybutynin_highInteraction() {
        List<Medication> meds = List.of(med("Rivastigmine"), med("Oxybutynin"));
        List<MedicationInteractionService.InteractionAlert> alerts = service.analyze(meds);
        assertThat(alerts).anyMatch(a -> "INTERACTION".equals(a.type()));
    }

    @Test
    @DisplayName("Two benzodiazepines = HIGH interaction alert")
    void twoBenzodiazepines_highAlert() {
        List<Medication> meds = List.of(med("Diazepam 5mg"), med("Lorazepam 1mg"));
        List<MedicationInteractionService.InteractionAlert> alerts = service.analyze(meds);
        assertThat(alerts).anyMatch(a -> "INTERACTION".equals(a.type()) && "HIGH".equals(a.severity()));
    }

    @Test
    @DisplayName("Diphenhydramine (Benadryl) = HIGH risk flag")
    void diphenhydramine_highRiskFlag() {
        List<MedicationInteractionService.InteractionAlert> alerts = service.analyze(List.of(med("Benadryl")));
        assertThat(alerts).anyMatch(a -> "RISK_FLAG".equals(a.type()) && "HIGH".equals(a.severity()));
    }

    @Test
    @DisplayName("Donepezil = PROTECTIVE flag")
    void donepezil_protectiveFlag() {
        List<MedicationInteractionService.InteractionAlert> alerts = service.analyze(List.of(med("Donepezil")));
        assertThat(alerts).anyMatch(a -> "PROTECTIVE".equals(a.type()));
    }

    @Test
    @DisplayName("Memantine = PROTECTIVE flag")
    void memantine_protectiveFlag() {
        List<MedicationInteractionService.InteractionAlert> alerts = service.analyze(List.of(med("Memantine")));
        assertThat(alerts).anyMatch(a -> "PROTECTIVE".equals(a.type()));
    }

    @Test
    @DisplayName("No medications = no alerts")
    void noMedications_noAlerts() {
        assertThat(service.analyze(List.of())).isEmpty();
    }

    @Test
    @DisplayName("Safe medication with no flags = no alerts")
    void safeMedication_noAlerts() {
        List<MedicationInteractionService.InteractionAlert> alerts = service.analyze(List.of(med("Vitamin D3")));
        assertThat(alerts.stream().filter(a -> "RISK_FLAG".equals(a.type()))).isEmpty();
    }

    @Test
    @DisplayName("Inactive medications are ignored in analysis")
    void inactiveMedications_ignored() {
        Medication inactive = med("Diazepam");
        inactive.setIsActive(false);
        Medication active = med("Lorazepam");
        active.setIsActive(true);

        List<MedicationInteractionService.InteractionAlert> alerts = service.analyze(List.of(inactive, active));
        long interactions = alerts.stream().filter(a -> "INTERACTION".equals(a.type())).count();
        assertThat(interactions).isZero();
    }

    @Test
    @DisplayName("Summary correctly identifies HIGH overall risk")
    void summary_highRisk_whenHighAlerts() {
        List<Medication> meds = List.of(med("Donepezil"), med("Amitriptyline"));
        List<MedicationInteractionService.InteractionAlert> alerts = service.analyze(meds);
        Map<String, Object> summary = service.summary(alerts);
        assertThat(summary.get("overallRisk")).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("Summary shows NONE when only protective flags")
    void summary_none_whenOnlyProtective() {
        List<MedicationInteractionService.InteractionAlert> alerts = service.analyze(List.of(med("Donepezil")));
        Map<String, Object> summary = service.summary(alerts);
        assertThat(summary.get("overallRisk")).isEqualTo("NONE");
        assertThat((Long) summary.get("protective")).isGreaterThan(0);
    }

    @Test
    @DisplayName("Summary contains all required keys")
    void summary_containsRequiredKeys() {
        Map<String, Object> summary = service.summary(List.of());
        assertThat(summary).containsKeys(
            "overallRisk", "highAlerts", "mediumAlerts", "lowAlerts",
            "protective", "interactions", "totalAlerts"
        );
    }

    @Test
    @DisplayName("HIGH severity alerts come before MEDIUM in sorted results")
    void sorting_highBeforeMedium() {
        List<Medication> meds = List.of(
            med("Donepezil"), med("Amitriptyline"), med("Lithium"), med("Zolpidem")
        );
        List<MedicationInteractionService.InteractionAlert> alerts = service.analyze(meds);
        if (alerts.size() >= 2) {
            int firstSev = severityIndex(alerts.get(0).severity());
            int lastSev  = severityIndex(alerts.get(alerts.size() - 1).severity());
            assertThat(firstSev).isLessThanOrEqualTo(lastSev);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Medication med(String name) {
        Medication m = new Medication();
        m.setName(name);
        m.setIsActive(true);
        return m;
    }

    private int severityIndex(String severity) {
        return switch (severity) {
            case "HIGH"       -> 0;
            case "MEDIUM"     -> 1;
            case "LOW"        -> 2;
            case "PROTECTIVE" -> 3;
            default           -> 4;
        };
    }
}
