package com.alzheimer.medicationadherence.repository;

import com.alzheimer.medicationadherence.entity.DoseStatus;
import com.alzheimer.medicationadherence.entity.MedicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicationLogRepository extends JpaRepository<MedicationLog, Long> {

    List<MedicationLog> findByPatientUserIdOrderByScheduledTimeDesc(Long patientUserId);

    List<MedicationLog> findByPatientUserIdAndScheduledDateOrderByScheduledTimeAsc(
            Long patientUserId, LocalDate date);

    /** Find all PENDING logs where scheduledTime has passed threshold — for missed dose detection */
    @Query("SELECT m FROM MedicationLog m WHERE m.status = 'PENDING' " +
           "AND m.scheduledTime <= :threshold AND m.alertSent = false")
    List<MedicationLog> findOverduePendingLogs(@Param("threshold") LocalDateTime threshold);

    /** Find MISSED logs that haven't had an alert sent yet */
    @Query("SELECT m FROM MedicationLog m WHERE m.status = 'MISSED' AND m.alertSent = false")
    List<MedicationLog> findMissedWithoutAlert();

    /** Logs within date range for adherence score calculation */
    @Query("SELECT m FROM MedicationLog m WHERE m.patientUserId = :patientId " +
           "AND m.scheduledDate >= :startDate AND m.scheduledDate <= :endDate")
    List<MedicationLog> findByPatientAndDateRange(@Param("patientId") Long patientId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    Optional<MedicationLog> findByPatientUserIdAndMedicationIdAndScheduledDate(
            Long patientUserId, Long medicationId, LocalDate scheduledDate);

    long countByPatientUserIdAndStatus(Long patientUserId, DoseStatus status);

    @Query("SELECT DISTINCT m.scheduledDate FROM MedicationLog m WHERE m.patientUserId = :patientId " +
           "AND m.scheduledDate >= :startDate ORDER BY m.scheduledDate DESC")
    List<LocalDate> findDistinctDates(@Param("patientId") Long patientId,
                                       @Param("startDate") LocalDate startDate);
}
