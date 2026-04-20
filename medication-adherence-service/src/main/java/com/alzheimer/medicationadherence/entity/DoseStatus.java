package com.alzheimer.medicationadherence.entity;

public enum DoseStatus {
    PENDING,   // not yet time, or time has passed but not yet flagged
    TAKEN,     // patient confirmed intake
    MISSED,    // overdue and not taken
    SKIPPED    // patient deliberately skipped (with reason)
}
