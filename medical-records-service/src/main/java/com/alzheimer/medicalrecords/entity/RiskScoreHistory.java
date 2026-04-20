package com.alzheimer.medicalrecords.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Immutable snapshot of a risk score calculation.
 *
 * Table: risk_score_history
 * The column medical_record_id stores the MedicalRecord PK.
 * Hibernate ddl-auto:update will create/alter the table automatically.
 *
 * This entity intentionally uses a plain Long FK (no @ManyToOne) so that
 * deleteByRecordId works cleanly without Hibernate cascade issues.
 */
@Entity
@Table(name = "risk_score_history", indexes = {
    @Index(name = "idx_rsh_record_id", columnList = "medical_record_id")
})
public class RiskScoreHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "medical_record_id", nullable = false)
    private Long recordId;

    @Column(name = "score", nullable = false)
    private Double score;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(name = "hereditary_contribution")
    private Double hereditaryContribution;

    @Column(name = "wellness_contribution")
    private Double wellnessContribution;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    /** What triggered this recalculation: RECORD_CREATED, RECORD_UPDATED,
     *  RISK_FACTOR_CHANGED, FAMILY_TREE_UPDATED, WELLNESS_PROFILE_UPDATED */
    @Column(name = "trigger_reason", length = 50)
    private String triggerReason;

    public RiskScoreHistory() {
        this.calculatedAt = LocalDateTime.now();
    }

    public RiskScoreHistory(Long recordId, Double score, RiskLevel riskLevel,
                             Double hereditaryContribution, Double wellnessContribution,
                             String triggerReason) {
        this.recordId               = recordId;
        this.score                  = score;
        this.riskLevel              = riskLevel;
        this.hereditaryContribution = hereditaryContribution;
        this.wellnessContribution   = wellnessContribution;
        this.triggerReason          = triggerReason;
        this.calculatedAt           = LocalDateTime.now();
    }

    public Long getId()                       { return id; }
    public void setId(Long id)                { this.id = id; }
    public Long getRecordId()                 { return recordId; }
    public void setRecordId(Long recordId)    { this.recordId = recordId; }
    public Double getScore()                  { return score; }
    public void setScore(Double score)        { this.score = score; }
    public RiskLevel getRiskLevel()           { return riskLevel; }
    public void setRiskLevel(RiskLevel v)     { this.riskLevel = v; }
    public Double getHereditaryContribution() { return hereditaryContribution; }
    public void setHereditaryContribution(Double v) { this.hereditaryContribution = v; }
    public Double getWellnessContribution()   { return wellnessContribution; }
    public void setWellnessContribution(Double v)   { this.wellnessContribution = v; }
    public LocalDateTime getCalculatedAt()    { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime v)    { this.calculatedAt = v; }
    public String getTriggerReason()          { return triggerReason; }
    public void setTriggerReason(String v)    { this.triggerReason = v; }
}
