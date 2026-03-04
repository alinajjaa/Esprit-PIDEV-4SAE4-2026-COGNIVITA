package com.planSuivi.planSuivi.controllers;



import com.planSuivi.planSuivi.model.PlanSuivi;
import com.planSuivi.planSuivi.services.PlanSuiviService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suivi")
public class PlanSuiviController {

    private final PlanSuiviService service;

    public PlanSuiviController(PlanSuiviService service) {
        this.service = service;
    }

    @PostMapping("/plans/from-rdv/{rdvId}")
    public PlanSuivi createFromRdv(@PathVariable Long rdvId) {
        return service.createPlanFromRdv(rdvId);
    }
    @GetMapping("/plans/patient/{patientId}")
    public List<PlanSuivi> getPlansByPatient(@PathVariable Long patientId) {
        return service.getPlansByPatient(patientId);
    }
}
