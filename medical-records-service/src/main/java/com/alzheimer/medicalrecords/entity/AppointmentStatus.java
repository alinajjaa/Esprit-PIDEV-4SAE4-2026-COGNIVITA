package com.alzheimer.medicalrecords.entity;

public enum AppointmentStatus {
    SCHEDULED("Scheduled"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    MISSED("Missed"),
    RESCHEDULED("Rescheduled");

    private final String displayName;
    AppointmentStatus(String displayName) { this.displayName = displayName; }
    public String getDisplayName()        { return displayName; }
}
