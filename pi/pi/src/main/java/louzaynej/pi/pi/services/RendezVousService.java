package louzaynej.pi.pi.services;

import louzaynej.pi.pi.dto.RendezVousSimpleDto;
import louzaynej.pi.pi.dto.RendezVousTableDto;
import louzaynej.pi.pi.exceptions.RendezVousNotFoundException;
import louzaynej.pi.pi.model.Medecin;
import louzaynej.pi.pi.model.Patient;
import louzaynej.pi.pi.model.RendezVous;
import louzaynej.pi.pi.model.RendezVousStatus;
import louzaynej.pi.pi.repositories.RendezVousRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RendezVousService {

    private final RendezVousRepository rendezVousRepository;
    private final EmailService emailService ;

    public RendezVousService(RendezVousRepository rendezVousRepository, EmailService emailService) {
        this.rendezVousRepository = rendezVousRepository;
        this.emailService = emailService;
    }
    private static final Duration RDV_DURATION = Duration.ofMinutes(30);
    public RendezVous createRendezVous(RendezVous rendezVous) {

        if (rendezVous.getMedecin() == null || rendezVous.getMedecin().getId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Médecin obligatoire");

        if (rendezVous.getPatient() == null || rendezVous.getPatient().getId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient obligatoire");

        if (rendezVous.getDateHeure() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date/heure obligatoire");

        Long medecinId = rendezVous.getMedecin().getId();
        Long patientId = rendezVous.getPatient().getId();

        LocalDateTime start = rendezVous.getDateHeure();
        LocalDateTime end = start.plus(RDV_DURATION);

        //  Search window: start-29min to end (enough to catch overlaps)
        LocalDateTime from = start.minusMinutes(29);
        LocalDateTime to = end;

        // ---- Doctor overlap (using eager-loading to prevent N+1 queries)
        List<RendezVous> doctorCandidates =
                rendezVousRepository.findByMedecinIdAndDateHeureBetweenEager(medecinId, from, to);

        for (RendezVous existing : doctorCandidates) {
            LocalDateTime eStart = existing.getDateHeure();
            LocalDateTime eEnd = eStart.plus(RDV_DURATION);

            if (start.isBefore(eEnd) && end.isAfter(eStart)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Créneau indisponible: le médecin a déjà un rendez-vous entre "
                                + eStart + " et " + eEnd
                );
            }
        }

        // ---- Optional: Patient overlap (using eager-loading to prevent N+1 queries)
        List<RendezVous> patientCandidates =
                rendezVousRepository.findByPatientIdAndDateHeureBetweenEager(patientId, from, to);

        for (RendezVous existing : patientCandidates) {
            LocalDateTime eStart = existing.getDateHeure();
            LocalDateTime eEnd = eStart.plus(RDV_DURATION);

            if (start.isBefore(eEnd) && end.isAfter(eStart)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Créneau indisponible: vous avez déjà un rendez-vous entre "
                                + eStart + " et " + eEnd
                );
            }
        }

        return rendezVousRepository.save(rendezVous);
    }
    public List<RendezVous> getAllRendezVous() {
        return rendezVousRepository.findAllEager();
    }
    public RendezVous getRendezVousById(Long id) {
        return rendezVousRepository.findById(id)
                .orElseThrow(() -> new RendezVousNotFoundException(id));
    }
    @Transactional
    public RendezVous updateRendezVous(Long id, RendezVous updatedRendezVous) {

        RendezVous existing = getRendezVousById(id);

        if (updatedRendezVous.getDateHeure() != null) {
            existing.setDateHeure(updatedRendezVous.getDateHeure());
        }

        if (updatedRendezVous.getStatus() != null) {
            existing.setStatus(updatedRendezVous.getStatus());
        }

        if (updatedRendezVous.getMedecin() != null) {
            existing.setMedecin(updatedRendezVous.getMedecin());
        }

        if (updatedRendezVous.getPatient() != null) {
            existing.setPatient(updatedRendezVous.getPatient());
        }

        return rendezVousRepository.save(existing);
    }

    @Transactional
    public void terminerRendezVous(Long id) {
        RendezVous existing = getRendezVousById(id);
        existing.setStatus(RendezVousStatus.TERMINE);
        rendezVousRepository.save(existing);
    }

    public void deleteRendezVous(Long id) {

        RendezVous rendezVous = getRendezVousById(id);
        rendezVousRepository.delete(rendezVous);
    }
    public List<RendezVous> getRendezVousByMedecin(Long medecinId) {
        return rendezVousRepository.findByMedecinIdEager(medecinId);
    }

    public List<RendezVous> getRendezVousByPatient(Long patientId) {
        return rendezVousRepository.findByPatientIdEager(patientId);
    }
    @Transactional
    public RendezVousSimpleDto confirmRendezVous(Long id) {

        RendezVous existing = getRendezVousById(id);

        // If already confirmed → return without sending email again
        if (existing.getStatus() == RendezVousStatus.CONFIRME) {
            return mapToSimpleDto(existing);
        }

        existing.setStatus(RendezVousStatus.CONFIRME);

        RendezVous saved = rendezVousRepository.save(existing);

        Patient patient = saved.getPatient();
        Medecin medecin = saved.getMedecin();

        // Send email safely
        try {
            if (patient != null && patient.getEmail() != null && !patient.getEmail().isBlank()) {
                emailService.sendRdvConfirmationHtml(
                        patient.getEmail(),
                        patient.getNomPatient(),
                        medecin != null ? medecin.getNomMedecin() : "Votre médecin",
                        saved.getDateHeure()
                );
            }
        } catch (Exception e) {
            System.out.println("Email sending failed: " + e.getMessage());
        }

        return mapToSimpleDto(saved);
    }
    @Transactional(readOnly = true)
    public List<RendezVousTableDto> getAllTable() {
        return rendezVousRepository.findAllEager()
                .stream()
                .map(this::mapToTableDto)
                .toList();
    }

    private RendezVousSimpleDto mapToSimpleDto(RendezVous rdv) {
        return new RendezVousSimpleDto(
                rdv.getId(),
                rdv.getDateHeure(),
                rdv.getStatus().name(),
                rdv.getPatient().getId()
        );
    }
    @Transactional
    public RendezVous assignCare(Long rdvId, louzaynej.pi.pi.dto.RendezVousCareAssignRequest req) {
        RendezVous rdv = getRendezVousById(rdvId);

        if (rdv.getStatus() != RendezVousStatus.TERMINE) {
            throw new RuntimeException("Cannot assign care until RDV is done");
        }

        rdv.setChambre(req.chambre());
        rdv.setInfermiere(req.infermiere());

        rdv.getMedicaments().clear();
        if (req.medicaments() != null) {
            rdv.getMedicaments().addAll(req.medicaments());
        }

        return rendezVousRepository.save(rdv);
    }
    private RendezVousTableDto mapToTableDto(RendezVous rdv) {
        return new RendezVousTableDto(
                rdv.getId(),
                rdv.getDateHeure(),
                rdv.getStatus().name(),

                rdv.getPatient() != null ? rdv.getPatient().getId() : null,
                rdv.getPatient() != null ? rdv.getPatient().getNomPatient() : null,

                rdv.getMedecin() != null ? rdv.getMedecin().getId() : null,
                rdv.getMedecin() != null ? rdv.getMedecin().getNomMedecin() : null,

                rdv.getChambre() != null ? rdv.getChambre().name() : null,
                rdv.getInfermiere() != null ? rdv.getInfermiere().name() : null,

                rdv.getMedicaments() != null
                        ? rdv.getMedicaments().stream().map(Enum::name).toList()
                        : List.of()
        );
    }
    @Transactional(readOnly = true)
    public List<RendezVousTableDto> getRendezVousByMedecinTable(Long medecinId) {
        return rendezVousRepository.findByMedecinIdEager(medecinId)
                .stream()
                .map(this::mapToTableDto)
                .toList();
    }
}

