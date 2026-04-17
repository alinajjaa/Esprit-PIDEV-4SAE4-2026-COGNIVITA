package com.alzheimer.medicalrecords.entity;

public enum APOEStatus {
    NOT_TESTED("Not Tested / Unknown",            0.0),
    E2_E2("ε2/ε2 — Protective",                  -8.0),
    E2_E3("ε2/ε3 — Slightly Protective",          -4.0),
    E3_E3("ε3/ε3 — Average Risk (Most Common)",    0.0),
    E2_E4("ε2/ε4 — Slightly Elevated Risk",        8.0),
    E3_E4("ε3/ε4 — Heterozygous (Elevated Risk)", 15.0),
    E4_E4("ε4/ε4 — Homozygous (High Risk)",       25.0);

    private final String displayName;
    private final double riskContribution;

    APOEStatus(String displayName, double riskContribution) {
        this.displayName      = displayName;
        this.riskContribution = riskContribution;
    }
    public String getDisplayName()      { return displayName; }
    public double getRiskContribution() { return riskContribution; }
}
