package louzaynej.pi.pi.controllers;


import louzaynej.pi.pi.dto.*;
import louzaynej.pi.pi.model.RendezVous;
import louzaynej.pi.pi.services.RendezVousService;
import louzaynej.pi.pi.services.SlotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/rendezvous")
public class RendezVousController {

    private final RendezVousService rendezVousService;
    private final SlotService slotService;



    public RendezVousController(RendezVousService rendezVousService, SlotService slotService) {
        this.rendezVousService = rendezVousService;
        this.slotService = slotService;
    }

    @PostMapping
    public RendezVous create(@RequestBody RendezVous rendezVous) {
        return rendezVousService.createRendezVous(rendezVous);
    }

    @GetMapping
    public List<RendezVous> getAll() {
        return rendezVousService.getAllRendezVous();
    }

    @GetMapping("/{id}")
    public RendezVous getById(@PathVariable Long id) {
        return rendezVousService.getRendezVousById(id);
    }

    @PutMapping("/{id}")
    public RendezVous update(@PathVariable Long id, @RequestBody RendezVous rendezVous) {
        return rendezVousService.updateRendezVous(id, rendezVous);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        rendezVousService.deleteRendezVous(id);
    }

    @GetMapping("/medecin/{medecinId}")
    public List<RendezVous> getByMedecin(@PathVariable Long medecinId) {
        return rendezVousService.getRendezVousByMedecin(medecinId);
    }

    @GetMapping("/patient/{patientId}")
    public List<RendezVous> getByPatient(@PathVariable Long patientId) {
        return rendezVousService.getRendezVousByPatient(patientId);
    }

    @GetMapping("/{id}/simple")
    public RendezVousSimpleDto getSimple(@PathVariable Long id) {
        RendezVous rdv = rendezVousService.getRendezVousById(id);

        return new RendezVousSimpleDto(
                rdv.getId(),
                rdv.getDateHeure(),
                rdv.getStatus().name(),
                rdv.getPatient().getId()
        );

    }

    @PutMapping("/{id}/confirm")
    public RendezVousSimpleDto confirm(@PathVariable Long id) {
        return rendezVousService.confirmRendezVous(id);
    }

    @PatchMapping("/{id}/terminate")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void terminate(@PathVariable Long id) {
        rendezVousService.terminerRendezVous(id);
    }

    @PatchMapping("/{id}/care")
    public RendezVous assignCare(@PathVariable Long id,
                                 @RequestBody RendezVousCareAssignRequest req) {
        return rendezVousService.assignCare(id, req);


    }
    @GetMapping("/table")
    public List<RendezVousTableDto> getAllTable () {
        return rendezVousService.getAllTable();
    }
    @GetMapping("/medecin/{medecinId}/table")
    public List<RendezVousTableDto> getByMedecinTable(@PathVariable Long medecinId) {
        return rendezVousService.getRendezVousByMedecinTable(medecinId);
    }
    @GetMapping("/medecin/{medecinId}/calendar")
    public List<RendezVousCalendarDto> getByMedecinCalendar(@PathVariable Long medecinId) {
        return rendezVousService.getRendezVousByMedecin(medecinId).stream()
                .map(r -> new RendezVousCalendarDto(
                        r.getId(),
                        r.getDateHeure(),
                        r.getStatus() != null ? r.getStatus().name() : null,
                        r.getPatient() != null ? r.getPatient().getNomPatient() : null,
                        r.getChambre() != null ? r.getChambre().name() : null
                ))
                .toList();
    }
    @GetMapping("/medecin/{medecinId}/next-available")
    public List<SlotDto> nextAvailable(
            @PathVariable Long medecinId,
            @RequestParam(defaultValue = "3") int count,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from
    ) {
        return slotService.nextAvailableSlots(medecinId, from, count);
    }

}
