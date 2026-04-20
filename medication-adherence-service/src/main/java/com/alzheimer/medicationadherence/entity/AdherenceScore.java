package com.alzheimer.medicationadherence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "adherence_scores", indexes = {
    @Index(name = "idx_adherence_patient", columnList = "patient_user_id", unique = true)
})
public class AdherenceScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_user_id", nullable = false, unique = true)
    private Long patientUserId;

    /** 0–100 score based on last 7 days */
    @Column(name = "weekly_score")
    private Double weeklyScore = 100.0;

    /** 0–100 score based on last 30 days */
    @Column(name = "monthly_score")
    private Double monthlyScore = 100.0;

    /** Current consecutive days with all doses taken */
    @Column(name = "streak_days")
    private Integer streakDays = 0;

    @Column(name = "total_doses_scheduled")
    private Integer totalDosesScheduled = 0;

    @Column(name = "total_doses_taken")
    private Integer totalDosesTaken = 0;

    @Column(name = "total_doses_missed")
    private Integer totalDosesMissed = 0;

    @Column(name = "last_calculated")
    private LocalDateTime lastCalculated;

    @Column(name = "last_taken_at")
    private LocalDateTime lastTakenAt;

    // ── Getters / Setters ────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPatientUserId() { return patientUserId; }
    public void setPatientUserId(Long patientUserId) { this.patientUserId = patientUserId; }
    public Double getWeeklyScore() { return weeklyScore; }
    public void setWeeklyScore(Double weeklyScore) { this.weeklyScore = weeklyScore; }
    public Double getMonthlyScore() { return monthlyScore; }
    public void setMonthlyScore(Double monthlyScore) { this.monthlyScore = monthlyScore; }
    public Integer getStreakDays() { return streakDays; }
    public void setStreakDays(Integer streakDays) { this.streakDays = streakDays; }
    public Integer getTotalDosesScheduled() { return totalDosesScheduled; }
    public void setTotalDosesScheduled(Integer totalDosesScheduled) { this.totalDosesScheduled = totalDosesScheduled; }
    public Integer getTotalDosesTaken() { return totalDosesTaken; }
    public void setTotalDosesTaken(Integer totalDosesTaken) { this.totalDosesTaken = totalDosesTaken; }
    public Integer getTotalDosesMissed() { return totalDosesMissed; }
    public void setTotalDosesMissed(Integer totalDosesMissed) { this.totalDosesMissed = totalDosesMissed; }
    public LocalDateTime getLastCalculated() { return lastCalculated; }
    public void setLastCalculated(LocalDateTime lastCalculated) { this.lastCalculated = lastCalculated; }
    public LocalDateTime getLastTakenAt() { return lastTakenAt; }
    public void setLastTakenAt(LocalDateTime lastTakenAt) { this.lastTakenAt = lastTakenAt; }
}
