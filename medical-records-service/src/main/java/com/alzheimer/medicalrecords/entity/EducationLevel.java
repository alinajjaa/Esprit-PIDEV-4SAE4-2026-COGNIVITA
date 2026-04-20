package com.alzheimer.medicalrecords.entity;

public enum EducationLevel {
    PRIMARY("Primary School"),
    SECONDARY("High School / Baccalaureate"),
    VOCATIONAL("Vocational / Technical"),
    BACHELOR("Bachelor's Degree"),
    MASTER("Master's Degree"),
    DOCTORATE("PhD / Doctorate");

    private final String displayName;
    EducationLevel(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
