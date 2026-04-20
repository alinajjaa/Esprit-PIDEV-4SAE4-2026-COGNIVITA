package com.alzheimer.notification.entity;

public enum NotificationStatus {
    PENDING,
    SENT,
    READ,     // IN_APP notifications transition here once the user reads them
    FAILED,
    SKIPPED   // channel disabled or no recipient configured
}
