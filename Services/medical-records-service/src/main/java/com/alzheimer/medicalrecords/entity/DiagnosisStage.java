package com.alzheimer.medicalrecords.entity;

public enum DiagnosisStage {
    PRECLINICAL("Preclinical",
        "No symptoms present. Biological changes may be occurring but cognitive function is normal.", 0, 20),
    MCI("Mild Cognitive Impairment (MCI)",
        "Noticeable memory or cognitive changes greater than expected for age, but not severe enough to interfere with daily life.", 20, 45),
    MILD("Mild Alzheimer's",
        "Memory lapses, confusion, difficulty with problem solving. Person can still manage most daily activities independently.", 45, 65),
    MODERATE("Moderate Alzheimer's",
        "Increased memory loss and confusion, difficulty recognizing family members, needs assistance with daily activities.", 65, 80),
    SEVERE("Severe Alzheimer's",
        "Loss of ability to respond to environment or carry on a conversation. Requires full-time care.", 80, 100);

    private final String displayName;
    private final String description;
    private final int minScore;
    private final int maxScore;

    DiagnosisStage(String displayName, String description, int minScore, int maxScore) {
        this.displayName = displayName;
        this.description = description;
        this.minScore    = minScore;
        this.maxScore    = maxScore;
    }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getMinScore()       { return minScore; }
    public int getMaxScore()       { return maxScore; }

    public static DiagnosisStage fromRiskScore(double score) {
        if (score < 20) return PRECLINICAL;
        if (score < 45) return MCI;
        if (score < 65) return MILD;
        if (score < 80) return MODERATE;
        return SEVERE;
    }
}
