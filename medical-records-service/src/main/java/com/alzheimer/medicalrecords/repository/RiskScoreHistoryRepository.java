package com.alzheimer.medicalrecords.repository;
import com.alzheimer.medicalrecords.repository.*;

import com.alzheimer.medicalrecords.entity.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RiskScoreHistoryRepository extends JpaRepository<RiskScoreHistory, Long> {

    List<RiskScoreHistory> findByRecordIdOrderByCalculatedAtDesc(Long recordId);

    @Query("SELECT h FROM RiskScoreHistory h WHERE h.recordId = :recordId ORDER BY h.calculatedAt ASC")
    List<RiskScoreHistory> findByRecordIdAsc(@Param("recordId") Long recordId);

    /** Bulk-delete all history for a record before the record itself is deleted. */
    @Modifying
    @Transactional
    @Query("DELETE FROM RiskScoreHistory h WHERE h.recordId = :recordId")
    void deleteByRecordId(@Param("recordId") Long recordId);
}
