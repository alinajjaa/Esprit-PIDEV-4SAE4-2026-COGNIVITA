package com.planSuivi.planSuivi.services;

import com.planSuivi.planSuivi.client.RendezVousClient;
import com.planSuivi.planSuivi.dto.RendezVousSimpleDto;
import com.planSuivi.planSuivi.model.*;
import com.planSuivi.planSuivi.repositories.PlanSuiviRepository;
import feign.FeignException;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlanSuiviService {

    private final PlanSuiviRepository planRepo;
    private final RendezVousClient rdvClient;

    public PlanSuiviService(PlanSuiviRepository planRepo, RendezVousClient rdvClient) {
        this.planRepo = planRepo;
        this.rdvClient = rdvClient;
    }

    @Transactional
    public PlanSuivi createPlanFromRdv(Long rdvId) {

        // 1) prevent duplicates
        planRepo.findByRdvId(rdvId).ifPresent(p -> {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Plan de suivi déjà existant pour rdvId=" + rdvId
            );
        });

        // 2) fetch RDV from MS-RDV
        RendezVousSimpleDto rdv;
        try {
            rdv = rdvClient.getSimpleById(rdvId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "RDV introuvable dans MS-RDV: " + rdvId
            );
        } catch (FeignException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Erreur lors de l'appel MS-RDV: " + e.status()
            );
        }

        // 3) validate payload
        if (rdv.getPatientId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "RDV invalide: patientId manquant pour rdvId=" + rdvId
            );
        }

        // 4) create plan
        PlanSuivi plan = new PlanSuivi();
        plan.setRdvId(rdv.getId());
        plan.setPatientId(rdv.getPatientId());
        plan.setRdvDate(rdv.getDateHeure());
        plan.setCreatedAt(LocalDateTime.now());
        plan.setStatus(PlanStatus.ACTIVE);

        // 5) generate default steps
        List<EtapeSuivi> steps = new ArrayList<>();
        steps.add(buildStep(plan, rdv.getDateHeure().plusDays(7),
                StepType.CHECKUP, "Contrôle à 7 jours", StepStatus.PENDING));

        steps.add(buildStep(plan, rdv.getDateHeure().plusDays(30),
                StepType.CONTROL_VISIT, "Suivi à 1 mois", StepStatus.PENDING));

        steps.add(buildStep(plan, rdv.getDateHeure().plusDays(90),
                StepType.CONTROL_VISIT, "Évaluation à 3 mois", StepStatus.PENDING));

        plan.setEtapes(steps);

        // 6) save (cascade saves steps)
        return planRepo.save(plan);
    }

    private EtapeSuivi buildStep(PlanSuivi plan,
                                 LocalDateTime when,
                                 StepType type,
                                 String title,
                                 StepStatus status) {

        EtapeSuivi s = new EtapeSuivi();
        s.setPlanSuivi(plan);
        s.setScheduledDate(when);
        s.setType(type);
        s.setTitle(title);
        s.setStatus(status);
        return s;
    }
    public List<PlanSuivi> getPlansByPatient(Long patientId) {
        return planRepo.findAllByPatientId(patientId);
    }
}