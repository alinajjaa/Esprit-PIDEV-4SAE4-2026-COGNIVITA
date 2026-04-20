package com.alzheimer.familytree.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "family_members")
public class FamilyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user whose family tree this belongs to */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "relationship", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Relationship relationship;

    @Column(name = "age")
    private Integer age;

    @Column(name = "is_alive")
    private Boolean isAlive = true;

    /** Whether this family member has / had Alzheimer's disease */
    @Column(name = "has_alzheimers")
    private Boolean hasAlzheimers = false;

    /** Whether this family member has / had dementia (general) */
    @Column(name = "has_dementia")
    private Boolean hasDementia = false;

    /** Other relevant medical conditions */
    @Column(name = "other_conditions", columnDefinition = "TEXT")
    private String otherConditions;

    /** The parent FamilyMember in this tree (null = root / the patient) */
    @Column(name = "parent_member_id")
    private Long parentMemberId;

    @Column(name = "gender", length = 10)
    @Enumerated(EnumType.STRING)
    private FamilyGender gender;

    /** Calculated hereditary risk contribution (0–100) */
    @Column(name = "hereditary_risk_score")
    private Double hereditaryRiskScore = 0.0;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Relationship getRelationship() { return relationship; }
    public void setRelationship(Relationship relationship) { this.relationship = relationship; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public Boolean getIsAlive() { return isAlive; }
    public void setIsAlive(Boolean isAlive) { this.isAlive = isAlive; }

    public Boolean getHasAlzheimers() { return hasAlzheimers; }
    public void setHasAlzheimers(Boolean hasAlzheimers) { this.hasAlzheimers = hasAlzheimers; }

    public Boolean getHasDementia() { return hasDementia; }
    public void setHasDementia(Boolean hasDementia) { this.hasDementia = hasDementia; }

    public String getOtherConditions() { return otherConditions; }
    public void setOtherConditions(String otherConditions) { this.otherConditions = otherConditions; }

    public Long getParentMemberId() { return parentMemberId; }
    public void setParentMemberId(Long parentMemberId) { this.parentMemberId = parentMemberId; }

    public FamilyGender getGender() { return gender; }
    public void setGender(FamilyGender gender) { this.gender = gender; }

    public Double getHereditaryRiskScore() { return hereditaryRiskScore; }
    public void setHereditaryRiskScore(Double hereditaryRiskScore) { this.hereditaryRiskScore = hereditaryRiskScore; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
