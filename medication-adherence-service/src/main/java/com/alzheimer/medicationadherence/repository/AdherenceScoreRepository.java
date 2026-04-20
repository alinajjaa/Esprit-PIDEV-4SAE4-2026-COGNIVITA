package com.alzheimer.medicationadherence.repository;

import com.alzheimer.medicationadherence.entity.AdherenceScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdherenceScoreRepository extends JpaRepository<AdherenceScore, Long> {
    Optional<AdherenceScore> findByPatientUserId(Long patientUserId);
}
