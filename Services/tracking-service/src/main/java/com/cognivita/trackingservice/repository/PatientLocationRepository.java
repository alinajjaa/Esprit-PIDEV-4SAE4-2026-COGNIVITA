package com.cognivita.trackingservice.repository;

import com.cognivita.trackingservice.entity.PatientLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientLocationRepository extends JpaRepository<PatientLocation, Long> {
    Optional<PatientLocation> findFirstByPatientIdAndMotionStateNotOrderByTimestampDesc(Long patientId, String motionState);
    Optional<PatientLocation> findFirstByPatientIdAndMotionStateOrderByTimestampAsc(Long patientId, String motionState);
    List<PatientLocation> findAllByOrderByTimestampDesc();
}
