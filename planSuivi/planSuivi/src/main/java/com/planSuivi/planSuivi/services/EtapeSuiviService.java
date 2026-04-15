package com.planSuivi.planSuivi.services;

import com.planSuivi.planSuivi.model.EtapeSuivi;
import com.planSuivi.planSuivi.model.StepStatus;
import com.planSuivi.planSuivi.repositories.EtapeSuiviRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class EtapeSuiviService {

    private final EtapeSuiviRepository etapeRepo;

    public EtapeSuiviService(EtapeSuiviRepository etapeRepo) {
        this.etapeRepo = etapeRepo;
    }

    @Transactional
    public EtapeSuivi markDone(Long stepId) {
        EtapeSuivi step = etapeRepo.findById(stepId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "EtapeSuivi introuvable: " + stepId
                ));

        if (step.getStatus() == StepStatus.DONE) {
            return step; // already done, idempotent
        }

        step.setStatus(StepStatus.DONE);
        step.setDoneAt(LocalDateTime.now());
        return etapeRepo.save(step);
    }
}