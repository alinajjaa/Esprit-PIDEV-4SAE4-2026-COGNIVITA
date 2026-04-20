package com.alzheimer.medicalrecords.repository;
import com.alzheimer.medicalrecords.repository.*;

import com.alzheimer.medicalrecords.entity.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MedicationRepository extends JpaRepository<Medication, Long> {

    List<Medication> findByMedicalRecordId(Long medicalRecordId);

    @Query("SELECT m FROM Medication m WHERE m.medicalRecord.id = :id AND m.isActive = true")
    List<Medication> findActiveByMedicalRecordId(@Param("id") Long id);

    @Query("SELECT COUNT(m) FROM Medication m WHERE m.medicalRecord.id = :id AND m.isActive = true")
    Long countActiveByMedicalRecordId(@Param("id") Long id);
}
