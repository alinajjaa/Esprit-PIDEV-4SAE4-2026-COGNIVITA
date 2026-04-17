package com.alzheimer.medicalrecords.entity;

public enum CognitiveSymptom {
    MEMORY_LOSS("Memory Loss", 2.5),
    MEMORY_IMPAIRMENT("Memory Impairment", 2.5),
    CONFUSION("Confusion / Disorientation", 2.5),
    GETTING_LOST("Getting Lost in Familiar Places", 2.5),
    WORD_FINDING_DIFFICULTY("Word Finding Difficulty / Aphasia", 2.5),
    PERSONALITY_CHANGE("Personality Change", 2.5),
    BEHAVIOURAL_CHANGE("Behavioural Change", 2.5),
    POOR_JUDGMENT("Poor Judgment / Decision Making", 1.5),
    SOCIAL_WITHDRAWAL("Social Withdrawal / Isolation", 1.5),
    MOOD_CHANGES("Mood Changes", 1.5),
    DEPRESSION("Depression", 1.5),
    ANXIETY("Anxiety", 1.5),
    DIFFICULTY_CONCENTRATING("Difficulty Concentrating", 1.5),
    SLEEP_DISTURBANCES("Sleep Disturbances / Insomnia", 1.0),
    FATIGUE("Unexplained Fatigue", 1.0),
    LOSS_OF_INITIATIVE("Loss of Initiative / Apathy", 1.0);

    private final String displayName;
    private final double riskWeight;

    CognitiveSymptom(String displayName, double riskWeight) {
        this.displayName = displayName;
        this.riskWeight = riskWeight;
    }
    public String getDisplayName() { return displayName; }
    public double getRiskWeight()  { return riskWeight; }
}
