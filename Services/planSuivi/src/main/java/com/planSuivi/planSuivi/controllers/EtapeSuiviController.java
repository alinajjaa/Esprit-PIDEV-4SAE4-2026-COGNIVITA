package com.planSuivi.planSuivi.controllers;

import com.planSuivi.planSuivi.model.EtapeSuivi;
import com.planSuivi.planSuivi.services.EtapeSuiviService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suivi")
public class EtapeSuiviController {

    private final EtapeSuiviService service;

    public EtapeSuiviController(EtapeSuiviService service) {
        this.service = service;
    }

    @PatchMapping("/steps/{stepId}/done")
    public EtapeSuivi markDone(@PathVariable Long stepId) {
        return service.markDone(stepId);
    }
}