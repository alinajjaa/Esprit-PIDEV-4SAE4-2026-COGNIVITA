package com.alzheimer.medicalrecords.service;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;

import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Analyzes a patient's active medication list for known Alzheimer's-relevant
 * interactions and cognitive risk implications.
 *
 * Two types of findings:
 *  - INTERACTION : Two specific drugs that interact negatively together
 *  - RISK_FLAG   : A single drug with known cognitive risk (anticholinergic load,
 *                  sedatives, antipsychotics, etc.)
 */
@Service
public class MedicationInteractionService {

    public record InteractionAlert(
        String type,         // "INTERACTION" | "RISK_FLAG" | "PROTECTIVE"
        String severity,     // "HIGH" | "MEDIUM" | "LOW"
        String drug1,
        String drug2,        // null for single-drug flags
        String message,
        String recommendation
    ) {}

    // Known pairs that should not be co-prescribed in Alzheimer's patients
    private static final List<DrugPair> INTERACTION_PAIRS = List.of(
        new DrugPair("donepezil",      "amitriptyline",    "HIGH",
            "Cholinesterase inhibitor + tricyclic antidepressant: additive anticholinergic effects may worsen cognition.",
            "Consider switching amitriptyline to an SSRI. Monitor closely."),
        new DrugPair("rivastigmine",   "oxybutynin",       "HIGH",
            "Cholinesterase inhibitor + anticholinergic bladder drug: opposing mechanisms reduce effectiveness of both.",
            "Consider mirabegron (non-anticholinergic) for bladder. Monitor cognition."),
        new DrugPair("galantamine",    "diphenhydramine",  "HIGH",
            "Cholinesterase inhibitor + antihistamine (Benadryl): strong anticholinergic antagonism reduces dementia drug efficacy.",
            "Avoid diphenhydramine entirely in patients on ChEI therapy. Use non-sedating antihistamines."),
        new DrugPair("memantine",      "ketamine",         "MEDIUM",
            "Both are NMDA antagonists — additive CNS effects may cause confusion, agitation.",
            "Avoid concurrent use. Consult psychiatry if ketamine infusions are planned."),
        new DrugPair("diazepam",       "lorazepam",        "HIGH",
            "Two benzodiazepines co-prescribed: cumulative CNS depression, high fall/confusion risk.",
            "Use only one benzodiazepine at the lowest effective dose. Taper plan recommended."),
        new DrugPair("haloperidol",    "quetiapine",       "HIGH",
            "Two antipsychotics co-prescribed: increased QT prolongation, extrapyramidal, metabolic risk.",
            "Monotherapy preferred. ECG monitoring required. Consult psychiatry."),
        new DrugPair("amitriptyline",  "diphenhydramine",  "MEDIUM",
            "Combined anticholinergic burden substantially increases dementia risk and causes confusion.",
            "Eliminate diphenhydramine. Review need for amitriptyline — SSRIs preferred."),
        new DrugPair("lithium",        "metformin",        "LOW",
            "Metformin may slightly reduce lithium clearance. Monitor lithium levels.",
            "Check serum lithium levels quarterly if both drugs are continued.")
    );

    // Single-drug cognitive risk flags
    private static final Map<String, DrugFlag> RISK_FLAGS = new LinkedHashMap<>();
    static {
        RISK_FLAGS.put("diphenhydramine", new DrugFlag("HIGH",
            "Strong anticholinergic — linked to 44% increased dementia risk with regular use.",
            "Replace with loratadine or cetirizine (non-anticholinergic antihistamines)."));
        RISK_FLAGS.put("benadryl", new DrugFlag("HIGH",
            "Diphenhydramine (Benadryl) — strong anticholinergic, blocks acetylcholine critical for memory.",
            "Avoid in patients 65+. Use non-sedating antihistamines."));
        RISK_FLAGS.put("diazepam", new DrugFlag("HIGH",
            "Benzodiazepine — associated with 50% increased risk of Alzheimer's with long-term use.",
            "Consider SSRI for anxiety. If essential, use short-acting agents at lowest dose with taper plan."));
        RISK_FLAGS.put("lorazepam", new DrugFlag("HIGH",
            "Benzodiazepine — sedation, memory impairment, fall risk especially in elderly.",
            "Use for short-term acute anxiety only. Non-benzo alternatives preferred."));
        RISK_FLAGS.put("alprazolam", new DrugFlag("HIGH",
            "Benzodiazepine — cognitive impairment with chronic use, rebound anxiety on withdrawal.",
            "Taper to discontinue if possible. CBT or SSRI as alternative."));
        RISK_FLAGS.put("zolpidem", new DrugFlag("MEDIUM",
            "Z-drug sleep aid — associated with cognitive decline, complex sleep behaviors, falls.",
            "CBT-I (Cognitive Behavioral Therapy for Insomnia) is first-line. Minimize duration."));
        RISK_FLAGS.put("amitriptyline", new DrugFlag("HIGH",
            "Tricyclic antidepressant with strong anticholinergic load — confirmed dementia risk.",
            "Switch to SSRI (sertraline, citalopram) or SNRI. Avoid in patients over 65."));
        RISK_FLAGS.put("oxybutynin", new DrugFlag("HIGH",
            "Anticholinergic bladder medication — crosses blood-brain barrier, significant cognitive risk.",
            "Switch to mirabegron (beta-3 agonist) for overactive bladder — no anticholinergic effect."));
        RISK_FLAGS.put("haloperidol", new DrugFlag("HIGH",
            "Typical antipsychotic — significantly increases mortality risk in elderly with dementia.",
            "Use atypical antipsychotics only when essential. Lowest dose, shortest duration."));
        RISK_FLAGS.put("quetiapine", new DrugFlag("MEDIUM",
            "Atypical antipsychotic — sedation, metabolic effects, may worsen cognition long-term.",
            "Monitor metabolic parameters. Reassess necessity every 3 months."));
        RISK_FLAGS.put("lithium", new DrugFlag("MEDIUM",
            "Narrow therapeutic index — toxicity causes confusion, ataxia. May reduce Alzheimer's risk at low doses.",
            "Monitor serum levels quarterly. Maintain 0.6-0.8 mEq/L for neuroprotective benefit."));
        // Protective flags
        RISK_FLAGS.put("donepezil", new DrugFlag("PROTECTIVE",
            "First-line cholinesterase inhibitor — FDA-approved for all stages of Alzheimer's.",
            "Continue as prescribed. Regular monitoring of heart rate (bradycardia risk)."));
        RISK_FLAGS.put("rivastigmine", new DrugFlag("PROTECTIVE",
            "Cholinesterase inhibitor approved for Alzheimer's and Parkinson's dementia.",
            "Transdermal patch preferred over oral for fewer GI side effects."));
        RISK_FLAGS.put("galantamine", new DrugFlag("PROTECTIVE",
            "Dual-action: cholinesterase inhibitor + nicotinic receptor modulator.",
            "Take with food. Monitor for nausea and bradycardia."));
        RISK_FLAGS.put("memantine", new DrugFlag("PROTECTIVE",
            "NMDA receptor antagonist — approved for moderate-to-severe Alzheimer's.",
            "Often combined with donepezil for synergistic effect."));
        RISK_FLAGS.put("metformin", new DrugFlag("PROTECTIVE",
            "Metformin (diabetes) may have neuroprotective effect by reducing insulin resistance in the brain.",
            "Continue if diabetes is present. Emerging evidence supports cognitive benefit."));
        RISK_FLAGS.put("atorvastatin", new DrugFlag("PROTECTIVE",
            "Statin — may reduce amyloid burden, anti-inflammatory cerebrovascular benefit.",
            "Continue for cardiovascular and potential neuroprotective benefit."));
        RISK_FLAGS.put("simvastatin", new DrugFlag("PROTECTIVE",
            "Statin — associated with reduced Alzheimer's risk in observational studies.",
            "Monitor for myopathy. Benefit likely highest in patients with elevated cholesterol."));
    }

    /**
     * Main analysis entry point.
     * Returns a list of alerts sorted by severity (HIGH first).
     */
    public List<InteractionAlert> analyze(List<Medication> medications) {
        List<Medication> active = medications.stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsActive()))
                .toList();

        List<InteractionAlert> alerts = new ArrayList<>();

        // Step 1: Check all pairs for interactions
        for (int i = 0; i < active.size(); i++) {
            for (int j = i + 1; j < active.size(); j++) {
                String a = active.get(i).getName().toLowerCase().trim();
                String b = active.get(j).getName().toLowerCase().trim();
                for (DrugPair pair : INTERACTION_PAIRS) {
                    if ((a.contains(pair.drug1) && b.contains(pair.drug2))
                     || (a.contains(pair.drug2) && b.contains(pair.drug1))) {
                        alerts.add(new InteractionAlert(
                            "INTERACTION", pair.severity,
                            active.get(i).getName(), active.get(j).getName(),
                            pair.message, pair.recommendation
                        ));
                    }
                }
            }
        }

        // Step 2: Check each drug for known cognitive risk/protective flags
        for (Medication med : active) {
            String lower = med.getName().toLowerCase().trim();
            for (Map.Entry<String, DrugFlag> entry : RISK_FLAGS.entrySet()) {
                if (lower.contains(entry.getKey())) {
                    String type = entry.getValue().severity.equals("PROTECTIVE") ? "PROTECTIVE" : "RISK_FLAG";
                    alerts.add(new InteractionAlert(
                        type, entry.getValue().severity,
                        med.getName(), null,
                        entry.getValue().message, entry.getValue().recommendation
                    ));
                    break;
                }
            }
        }

        // Sort: HIGH first, then MEDIUM, PROTECTIVE last
        alerts.sort(Comparator.comparingInt(a -> {
            return switch (a.severity()) {
                case "HIGH"        -> 0;
                case "MEDIUM"      -> 1;
                case "LOW"         -> 2;
                case "PROTECTIVE"  -> 3;
                default            -> 4;
            };
        }));

        return alerts;
    }

    /** Summary: counts of HIGH/MEDIUM/LOW/PROTECTIVE and overall risk level */
    public Map<String, Object> summary(List<InteractionAlert> alerts) {
        long high        = alerts.stream().filter(a -> "HIGH".equals(a.severity()) && !"PROTECTIVE".equals(a.type())).count();
        long medium      = alerts.stream().filter(a -> "MEDIUM".equals(a.severity())).count();
        long low         = alerts.stream().filter(a -> "LOW".equals(a.severity())).count();
        long protective  = alerts.stream().filter(a -> "PROTECTIVE".equals(a.type())).count();
        long interactions = alerts.stream().filter(a -> "INTERACTION".equals(a.type())).count();

        String overallRisk = high > 0 ? "HIGH" : medium > 0 ? "MEDIUM" : low > 0 ? "LOW" : "NONE";

        Map<String, Object> s = new LinkedHashMap<>();
        s.put("overallRisk",    overallRisk);
        s.put("highAlerts",     high);
        s.put("mediumAlerts",   medium);
        s.put("lowAlerts",      low);
        s.put("protective",     protective);
        s.put("interactions",   interactions);
        s.put("totalAlerts",    alerts.size());
        return s;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private record DrugPair(String drug1, String drug2, String severity, String message, String recommendation) {}
    private record DrugFlag(String severity, String message, String recommendation) {}
}
