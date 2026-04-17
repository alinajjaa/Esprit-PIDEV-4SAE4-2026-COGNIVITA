package com.planSuivi.planSuivi.repositories;


import com.planSuivi.planSuivi.model.PlanStatus;
import com.planSuivi.planSuivi.model.PlanSuivi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanSuiviRepository extends JpaRepository<PlanSuivi, Long> {

    // Useful to prevent creating 2 plans for the same rdv
    Optional<PlanSuivi> findByRdvId(Long rdvId);

    // Fetch all plans for a patient
    List<PlanSuivi> findAllByPatientId(Long patientId);

    // Optional: filter by status
    List<PlanSuivi> findAllByPatientIdAndStatus(Long patientId, PlanStatus status);
}