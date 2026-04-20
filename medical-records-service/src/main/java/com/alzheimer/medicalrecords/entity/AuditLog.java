package com.alzheimer.medicalrecords.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Immutable record of every significant data operation.
 * Created by AuditService — never updated, only inserted and queried.
 */
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_entity",   columnList = "entity_type, entity_id"),
    @Index(name = "idx_audit_occurred", columnList = "occurred_at DESC"),
    @Index(name = "idx_audit_performed",columnList = "performed_by")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;   // "MedicalRecord", "Appointment", "Medication", etc.

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;       // "CREATE", "UPDATE", "DELETE", "VIEW", "EXPORT_PDF"

    @Column(name = "performed_by", length = 200)
    private String performedBy;  // email or "SYSTEM"

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "changes_json", columnDefinition = "TEXT")
    private String changesJson;  // JSON summary of what changed (or null)

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();

    public AuditLog() {}

    public AuditLog(String entityType, Long entityId, String action,
                    String performedBy, String changesJson) {
        this.entityType  = entityType;
        this.entityId    = entityId;
        this.action      = action;
        this.performedBy = performedBy;
        this.changesJson = changesJson;
        this.occurredAt  = LocalDateTime.now();
    }

    public Long getId()                        { return id; }
    public String getEntityType()              { return entityType; }
    public void setEntityType(String v)        { this.entityType = v; }
    public Long getEntityId()                  { return entityId; }
    public void setEntityId(Long v)            { this.entityId = v; }
    public String getAction()                  { return action; }
    public void setAction(String v)            { this.action = v; }
    public String getPerformedBy()             { return performedBy; }
    public void setPerformedBy(String v)       { this.performedBy = v; }
    public String getIpAddress()               { return ipAddress; }
    public void setIpAddress(String v)         { this.ipAddress = v; }
    public String getChangesJson()             { return changesJson; }
    public void setChangesJson(String v)       { this.changesJson = v; }
    public LocalDateTime getOccurredAt()       { return occurredAt; }
    public void setOccurredAt(LocalDateTime v) { this.occurredAt = v; }
}
