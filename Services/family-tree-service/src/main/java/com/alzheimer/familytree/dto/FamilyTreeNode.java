package com.alzheimer.familytree.dto;

import java.util.ArrayList;
import java.util.List;
import com.alzheimer.familytree.entity.FamilyMember;
import com.alzheimer.familytree.entity.Relationship;
import com.alzheimer.familytree.entity.FamilyGender;

/**
 * DTO used for building the visual family-tree graph in the frontend.
 * Each node references its children (BFS/DFS traversal ready).
 */
public class FamilyTreeNode {

    private Long id;
    private String fullName;
    private String relationship;
    private Integer age;
    private boolean isAlive;
    private boolean hasAlzheimers;
    private boolean hasDementia;
    private String gender;
    private Double hereditaryRiskScore;
    private String otherConditions;
    private String notes;
    private Long parentMemberId;
    private List<FamilyTreeNode> children = new ArrayList<>();

    public FamilyTreeNode() {}

    public FamilyTreeNode(FamilyMember member) {
        this.id = member.getId();
        this.fullName = member.getFullName();
        this.relationship = member.getRelationship() != null ? member.getRelationship().name() : null;
        this.age = member.getAge();
        this.isAlive = Boolean.TRUE.equals(member.getIsAlive());
        this.hasAlzheimers = Boolean.TRUE.equals(member.getHasAlzheimers());
        this.hasDementia = Boolean.TRUE.equals(member.getHasDementia());
        this.gender = member.getGender() != null ? member.getGender().name() : null;
        this.hereditaryRiskScore = member.getHereditaryRiskScore();
        this.otherConditions = member.getOtherConditions();
        this.notes = member.getNotes();
        this.parentMemberId = member.getParentMemberId();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public boolean isAlive() { return isAlive; }
    public void setAlive(boolean alive) { isAlive = alive; }

    public boolean isHasAlzheimers() { return hasAlzheimers; }
    public void setHasAlzheimers(boolean hasAlzheimers) { this.hasAlzheimers = hasAlzheimers; }

    public boolean isHasDementia() { return hasDementia; }
    public void setHasDementia(boolean hasDementia) { this.hasDementia = hasDementia; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Double getHereditaryRiskScore() { return hereditaryRiskScore; }
    public void setHereditaryRiskScore(Double hereditaryRiskScore) { this.hereditaryRiskScore = hereditaryRiskScore; }

    public String getOtherConditions() { return otherConditions; }
    public void setOtherConditions(String otherConditions) { this.otherConditions = otherConditions; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getParentMemberId() { return parentMemberId; }
    public void setParentMemberId(Long parentMemberId) { this.parentMemberId = parentMemberId; }

    public List<FamilyTreeNode> getChildren() { return children; }
    public void setChildren(List<FamilyTreeNode> children) { this.children = children; }
}
