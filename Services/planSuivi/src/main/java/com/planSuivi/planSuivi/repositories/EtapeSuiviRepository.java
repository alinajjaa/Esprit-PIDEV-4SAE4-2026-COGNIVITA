package com.planSuivi.planSuivi.repositories;
import com.planSuivi.planSuivi.model.EtapeSuivi;
import com.planSuivi.planSuivi.model.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EtapeSuiviRepository extends JpaRepository<EtapeSuivi, Long> {

    // Get steps of a plan (for plan details page)
    List<EtapeSuivi> findAllByPlanSuivi_IdOrderByScheduledDateAsc(Long planId);

    // Optional: pending steps of a plan
    List<EtapeSuivi> findAllByPlanSuivi_IdAndStatusOrderByScheduledDateAsc(Long planId, StepStatus status);

    // Optional: for future "automatic check" (overdue steps)
    List<EtapeSuivi> findAllByStatusAndScheduledDateBefore(StepStatus status, LocalDateTime dateTime);
}