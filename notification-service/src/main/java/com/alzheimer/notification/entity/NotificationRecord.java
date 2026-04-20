package com.alzheimer.notification.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_user_id",  columnList = "user_id"),
    @Index(name = "idx_notif_type",     columnList = "notification_type"),
    @Index(name = "idx_notif_status",   columnList = "status"),
    @Index(name = "idx_notif_channel",  columnList = "channel"),
    @Index(name = "idx_notif_created",  columnList = "created_at DESC")
})
public class NotificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Optional: ties back to a medical record, medication log, appointment, etc. */
    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "notification_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    @Column(name = "channel", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;

    @Column(name = "severity", length = 20)
    private String severity; // INFO, WARNING, CRITICAL

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "recipient_email", length = 200)
    private String recipientEmail;

    @Column(name = "recipient_phone", length = 30)
    private String recipientPhone;

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** For escalation chain: 0=patient, 1=caregiver, 2=doctor */
    @Column(name = "escalation_level")
    private Integer escalationLevel = 0;

    @Column(name = "escalation_group_id", length = 100)
    private String escalationGroupId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Getters / Setters ────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public NotificationType getNotificationType() { return notificationType; }
    public void setNotificationType(NotificationType notificationType) { this.notificationType = notificationType; }
    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }
    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Integer getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(Integer escalationLevel) { this.escalationLevel = escalationLevel; }
    public String getEscalationGroupId() { return escalationGroupId; }
    public void setEscalationGroupId(String escalationGroupId) { this.escalationGroupId = escalationGroupId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
