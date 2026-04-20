package com.alzheimer.medicalrecords.repository;
import com.alzheimer.medicalrecords.repository.*;

import com.alzheimer.medicalrecords.entity.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RiskFactorRepository extends JpaRepository<RiskFactor, Long> {

    Page<RiskFactor> findByMedicalRecordId(Long medicalRecordId, Pageable pageable);

    List<RiskFactor> findByMedicalRecordIdAndIsActive(Long medicalRecordId, Boolean isActive);

    @Query("SELECT COUNT(rf) FROM RiskFactor rf WHERE rf.medicalRecord.id = :medicalRecordId")
    Long countByMedicalRecordId(@Param("medicalRecordId") Long medicalRecordId);

    @Query("SELECT COUNT(rf) FROM RiskFactor rf WHERE rf.medicalRecord.id = :medicalRecordId AND rf.isActive = :isActive")
    Long countByMedicalRecordIdAndIsActive(@Param("medicalRecordId") Long medicalRecordId, @Param("isActive") Boolean isActive);

    List<RiskFactor> findByMedicalRecordId(Long medicalRecordId);

    /** JPQL group-by severity for active risk factors */
    @Query("SELECT rf.severity, COUNT(rf) FROM RiskFactor rf WHERE rf.medicalRecord.id = :medicalRecordId AND rf.isActive = true GROUP BY rf.severity")
    List<Object[]> countActiveBySeverityForRecord(@Param("medicalRecordId") Long medicalRecordId);
}
