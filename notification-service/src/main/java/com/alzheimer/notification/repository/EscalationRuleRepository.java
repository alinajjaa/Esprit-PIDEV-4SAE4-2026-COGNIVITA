package com.alzheimer.notification.repository;

import com.alzheimer.notification.entity.EscalationRule;
import com.alzheimer.notification.entity.EscalationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EscalationRuleRepository extends JpaRepository<EscalationRule, Long> {

    List<EscalationRule> findByActiveTrue();

    Optional<EscalationRule> findByPatientUserIdAndEscalationTypeAndReferenceIdAndActiveTrue(
            Long patientUserId, EscalationType type, Long referenceId);

    List<EscalationRule> findByPatientUserIdOrderByTriggeredAtDesc(Long patientUserId);

    /**
     * FIX: was "currentLevel < 2" which returned level-0 AND level-1 rules into the
     * same list, causing the scheduler's level-0 loop to silently skip level-1 rows
     * (harmless but misleading) and potentially miss them entirely when lastEscalatedAt
     * was null. Now strictly returns only level-0 (not-yet-escalated) active rules
     * whose triggeredAt is older than the given threshold.
     */
    @Query("SELECT e FROM EscalationRule e WHERE e.active = true AND e.currentLevel = 0 " +
           "AND e.triggeredAt <= :threshold")
    List<EscalationRule> findOverdueEscalations(@Param("threshold") LocalDateTime threshold);

    /**
     * Returns active level-1 (caregiver-notified) rules whose lastEscalatedAt is
     * older than the given threshold, ready to be escalated to doctor.
     */
    @Query("SELECT e FROM EscalationRule e WHERE e.active = true AND e.currentLevel = 1 " +
           "AND e.lastEscalatedAt <= :threshold")
    List<EscalationRule> findCaregiverEscalationsOverdue(@Param("threshold") LocalDateTime threshold);
}
