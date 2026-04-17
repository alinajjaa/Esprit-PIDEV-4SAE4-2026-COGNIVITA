package com.alzheimer.medicalrecords.repository;
import com.alzheimer.medicalrecords.repository.*;

import com.alzheimer.medicalrecords.entity.*;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CareNoteRepository extends JpaRepository<CareNote, Long> {
    List<CareNote> findByMedicalRecordIdOrderByIsPinnedDescCreatedAtDesc(Long medicalRecordId);
    Long countByMedicalRecordId(Long medicalRecordId);
}
