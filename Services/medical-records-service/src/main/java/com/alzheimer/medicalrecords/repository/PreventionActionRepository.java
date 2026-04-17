package com.alzheimer.medicalrecords.repository;
import com.alzheimer.medicalrecords.repository.*;

import com.alzheimer.medicalrecords.entity.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PreventionActionRepository extends JpaRepository<PreventionAction, Long> {

    Page<PreventionAction> findByMedicalRecordId(Long medicalRecordId, Pageable pageable);

    List<PreventionAction> findByMedicalRecordIdAndStatus(Long medicalRecordId, ActionStatus status);

    List<PreventionAction> findByMedicalRecordId(Long medicalRecordId);

    @Query("SELECT COUNT(pa) FROM PreventionAction pa WHERE pa.medicalRecord.id = :medicalRecordId")
    Long countByMedicalRecordId(@Param("medicalRecordId") Long medicalRecordId);

    @Query("SELECT COUNT(pa) FROM PreventionAction pa WHERE pa.medicalRecord.id = :medicalRecordId AND pa.status = :status")
    Long countByMedicalRecordIdAndStatus(@Param("medicalRecordId") Long medicalRecordId,
                                          @Param("status") ActionStatus status);

    List<PreventionAction> findByMedicalRecordIdAndActionDateBetween(
            Long medicalRecordId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * FIX: The original JPQL used LPAD(CAST(... AS string), ...) which is not valid
     * Hibernate JPQL syntax and causes startup/query failures.
     * Native SQL is used instead — DATE_FORMAT works directly in MySQL and
     * produces the same "YYYY-MM" grouping.
     */
    @Query(value = "SELECT DATE_FORMAT(pa.action_date, '%Y-%m') AS month, COUNT(*) AS cnt " +
                   "FROM prevention_actions pa " +
                   "WHERE pa.medical_record_id = :medicalRecordId " +
                   "GROUP BY DATE_FORMAT(pa.action_date, '%Y-%m') " +
                   "ORDER BY month ASC",
           nativeQuery = true)
    List<Object[]> countActionsByMonthForRecord(@Param("medicalRecordId") Long medicalRecordId);
}
