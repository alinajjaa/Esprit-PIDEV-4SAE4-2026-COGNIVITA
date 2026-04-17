package com.alzheimer.medicalrecords.entity;

public enum AppointmentType {
    GENERAL("General Check-up"),
    NEUROLOGIST("Neurologist"),
    MEMORY_CLINIC("Memory Clinic"),
    GENETICS("Genetics / APOE Testing"),
    PHARMACIST("Pharmacist / Medication Review"),
    FOLLOW_UP("Follow-up"),
    IMAGING("Brain Imaging (MRI/CT)"),
    PSYCHIATRY("Psychiatry");

    private final String displayName;
    AppointmentType(String displayName) { this.displayName = displayName; }
    public String getDisplayName()      { return displayName; }
}
