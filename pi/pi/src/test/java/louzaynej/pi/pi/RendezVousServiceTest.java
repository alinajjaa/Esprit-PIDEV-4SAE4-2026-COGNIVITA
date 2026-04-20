package louzaynej.pi.pi;

import louzaynej.pi.pi.dto.RendezVousCareAssignRequest;
import louzaynej.pi.pi.dto.RendezVousSimpleDto;
import louzaynej.pi.pi.dto.RendezVousTableDto;
import louzaynej.pi.pi.exceptions.RendezVousNotFoundException;
import louzaynej.pi.pi.model.*;
import louzaynej.pi.pi.repositories.RendezVousRepository;
import louzaynej.pi.pi.services.EmailService;
import louzaynej.pi.pi.services.RendezVousService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RendezVousServiceTest {

    @Mock
    private RendezVousRepository rendezVousRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private RendezVousService rendezVousService;

    // ─────────────────────────────────────────────────────────────
    // getRendezVousById
    // ─────────────────────────────────────────────────────────────

    @Test
    void getRendezVousById_whenExists_returnsRdv() {
        RendezVous rdv = new RendezVous();
        rdv.setId(1L);

        when(rendezVousRepository.findById(1L)).thenReturn(Optional.of(rdv));

        RendezVous result = rendezVousService.getRendezVousById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(rendezVousRepository).findById(1L);
    }

    @Test
    void getRendezVousById_whenNotExists_throwsException() {
        when(rendezVousRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RendezVousNotFoundException.class,
                () -> rendezVousService.getRendezVousById(99L));

        verify(rendezVousRepository).findById(99L);
    }

    // ─────────────────────────────────────────────────────────────
    // getAllRendezVous
    // ─────────────────────────────────────────────────────────────

    @Test
    void getAllRendezVous_returnsList() {
        RendezVous rdv1 = new RendezVous();
        rdv1.setId(1L);
        RendezVous rdv2 = new RendezVous();
        rdv2.setId(2L);

        when(rendezVousRepository.findAll()).thenReturn(List.of(rdv1, rdv2));

        List<RendezVous> result = rendezVousService.getAllRendezVous();

        assertEquals(2, result.size());
        verify(rendezVousRepository).findAll();
    }

    @Test
    void getAllRendezVous_whenEmpty_returnsEmptyList() {
        when(rendezVousRepository.findAll()).thenReturn(List.of());

        List<RendezVous> result = rendezVousService.getAllRendezVous();

        assertTrue(result.isEmpty());
        verify(rendezVousRepository).findAll();
    }

    // ─────────────────────────────────────────────────────────────
    // createRendezVous
    // ─────────────────────────────────────────────────────────────

    @Test
    void createRendezVous_valid_savesAndReturns() {
        Medecin medecin = new Medecin();
        medecin.setId(1L);

        Patient patient = new Patient();
        patient.setId(2L);

        LocalDateTime dateHeure = LocalDateTime.of(2026, 6, 1, 10, 0);

        RendezVous rdv = new RendezVous();
        rdv.setMedecin(medecin);
        rdv.setPatient(patient);
        rdv.setDateHeure(dateHeure);

        when(rendezVousRepository.findByMedecinIdAndDateHeureBetween(anyLong(), any(), any()))
                .thenReturn(List.of());
        when(rendezVousRepository.findByPatientIdAndDateHeureBetween(anyLong(), any(), any()))
                .thenReturn(List.of());
        when(rendezVousRepository.save(rdv)).thenReturn(rdv);

        RendezVous result = rendezVousService.createRendezVous(rdv);

        assertNotNull(result);
        verify(rendezVousRepository).save(rdv);
    }

    @Test
    void createRendezVous_withoutMedecin_throwsBadRequest() {
        RendezVous rdv = new RendezVous();
        rdv.setPatient(new Patient());
        rdv.setDateHeure(LocalDateTime.now());
        // medecin is null

        assertThrows(ResponseStatusException.class,
                () -> rendezVousService.createRendezVous(rdv));

        verify(rendezVousRepository, never()).save(any());
    }

    @Test
    void createRendezVous_withoutPatient_throwsBadRequest() {
        Medecin medecin = new Medecin();
        medecin.setId(1L);

        RendezVous rdv = new RendezVous();
        rdv.setMedecin(medecin);
        // patient is null

        assertThrows(ResponseStatusException.class,
                () -> rendezVousService.createRendezVous(rdv));

        verify(rendezVousRepository, never()).save(any());
    }

    @Test
    void createRendezVous_withoutDate_throwsBadRequest() {
        Medecin medecin = new Medecin();
        medecin.setId(1L);
        Patient patient = new Patient();
        patient.setId(2L);

        RendezVous rdv = new RendezVous();
        rdv.setMedecin(medecin);
        rdv.setPatient(patient);
        // dateHeure is null

        assertThrows(ResponseStatusException.class,
                () -> rendezVousService.createRendezVous(rdv));

        verify(rendezVousRepository, never()).save(any());
    }

    @Test
    void createRendezVous_doctorConflict_throwsConflict() {
        Medecin medecin = new Medecin();
        medecin.setId(1L);
        Patient patient = new Patient();
        patient.setId(2L);

        LocalDateTime dateHeure = LocalDateTime.of(2026, 6, 1, 10, 0);

        RendezVous newRdv = new RendezVous();
        newRdv.setMedecin(medecin);
        newRdv.setPatient(patient);
        newRdv.setDateHeure(dateHeure);

        // Existing overlapping appointment for the same doctor
        RendezVous existing = new RendezVous();
        existing.setDateHeure(dateHeure); // exact same time → overlaps

        when(rendezVousRepository.findByMedecinIdAndDateHeureBetween(anyLong(), any(), any()))
                .thenReturn(List.of(existing));

        assertThrows(ResponseStatusException.class,
                () -> rendezVousService.createRendezVous(newRdv));

        verify(rendezVousRepository, never()).save(any());
    }

    @Test
    void createRendezVous_patientConflict_throwsConflict() {
        Medecin medecin = new Medecin();
        medecin.setId(1L);
        Patient patient = new Patient();
        patient.setId(2L);

        LocalDateTime dateHeure = LocalDateTime.of(2026, 6, 1, 10, 0);

        RendezVous newRdv = new RendezVous();
        newRdv.setMedecin(medecin);
        newRdv.setPatient(patient);
        newRdv.setDateHeure(dateHeure);

        RendezVous existingForPatient = new RendezVous();
        existingForPatient.setDateHeure(dateHeure);

        when(rendezVousRepository.findByMedecinIdAndDateHeureBetween(anyLong(), any(), any()))
                .thenReturn(List.of());
        when(rendezVousRepository.findByPatientIdAndDateHeureBetween(anyLong(), any(), any()))
                .thenReturn(List.of(existingForPatient));

        assertThrows(ResponseStatusException.class,
                () -> rendezVousService.createRendezVous(newRdv));

        verify(rendezVousRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────
    // updateRendezVous
    // ─────────────────────────────────────────────────────────────

    @Test
    void updateRendezVous_updatesFieldsAndSaves() {
        Patient patient = new Patient();
        patient.setId(5L);

        RendezVous existing = new RendezVous();
        existing.setId(1L);
        existing.setStatus(RendezVousStatus.PLANIFIE);
        existing.setPatient(patient);

        LocalDateTime newDate = LocalDateTime.of(2026, 7, 15, 9, 0);

        RendezVous update = new RendezVous();
        update.setDateHeure(newDate);
        update.setStatus(RendezVousStatus.CONFIRME);

        when(rendezVousRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(rendezVousRepository.save(any(RendezVous.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RendezVous result = rendezVousService.updateRendezVous(1L, update);

        assertEquals(newDate, result.getDateHeure());
        assertEquals(RendezVousStatus.CONFIRME, result.getStatus());
        verify(rendezVousRepository).save(existing);
    }

    @Test
    void updateRendezVous_whenNotFound_throwsException() {
        when(rendezVousRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RendezVousNotFoundException.class,
                () -> rendezVousService.updateRendezVous(99L, new RendezVous()));

        verify(rendezVousRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────
    // deleteRendezVous
    // ─────────────────────────────────────────────────────────────

    @Test
    void deleteRendezVous_whenExists_deletesIt() {
        RendezVous rdv = new RendezVous();
        rdv.setId(1L);

        when(rendezVousRepository.findById(1L)).thenReturn(Optional.of(rdv));

        rendezVousService.deleteRendezVous(1L);

        verify(rendezVousRepository).delete(rdv);
    }

    @Test
    void deleteRendezVous_whenNotFound_throwsException() {
        when(rendezVousRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RendezVousNotFoundException.class,
                () -> rendezVousService.deleteRendezVous(99L));

        verify(rendezVousRepository, never()).delete(any());
    }

    // ─────────────────────────────────────────────────────────────
    // getRendezVousByMedecin / getRendezVousByPatient
    // ─────────────────────────────────────────────────────────────

    @Test
    void getRendezVousByMedecin_returnsList() {
        RendezVous rdv = new RendezVous();
        rdv.setId(1L);

        when(rendezVousRepository.findByMedecinId(10L)).thenReturn(List.of(rdv));

        List<RendezVous> result = rendezVousService.getRendezVousByMedecin(10L);

        assertEquals(1, result.size());
        verify(rendezVousRepository).findByMedecinId(10L);
    }

    @Test
    void getRendezVousByPatient_returnsList() {
        RendezVous rdv = new RendezVous();
        rdv.setId(2L);

        when(rendezVousRepository.findByPatientId(20L)).thenReturn(List.of(rdv));

        List<RendezVous> result = rendezVousService.getRendezVousByPatient(20L);

        assertEquals(1, result.size());
        verify(rendezVousRepository).findByPatientId(20L);
    }

    // ─────────────────────────────────────────────────────────────
    // confirmRendezVous
    // ─────────────────────────────────────────────────────────────

    @Test
    void confirmRendezVous_whenNotConfirmed_updatesStatus_andSendsEmail() {

        Patient patient = new Patient();
        patient.setId(10L);
        patient.setNomPatient("Ali");
        patient.setEmail("ali@test.com");

        Medecin medecin = new Medecin();
        medecin.setNomMedecin("Dr House");

        RendezVous rdv = new RendezVous();
        rdv.setId(1L);
        rdv.setStatus(RendezVousStatus.PLANIFIE);
        rdv.setDateHeure(LocalDateTime.of(2026, 2, 25, 10, 0));
        rdv.setPatient(patient);
        rdv.setMedecin(medecin);

        when(rendezVousRepository.findById(1L)).thenReturn(Optional.of(rdv));
        when(rendezVousRepository.save(any(RendezVous.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RendezVousSimpleDto dto = rendezVousService.confirmRendezVous(1L);

        assertEquals("CONFIRME", dto.getStatus());
        assertEquals(10L, dto.getPatientId());
        assertEquals(1L, dto.getId());

        verify(rendezVousRepository).findById(1L);
        verify(rendezVousRepository).save(any(RendezVous.class));

        verify(emailService).sendRdvConfirmationHtml(
                eq("ali@test.com"),
                eq("Ali"),
                eq("Dr House"),
                any(LocalDateTime.class)
        );
    }

    @Test
    void confirmRendezVous_whenAlreadyConfirmed_doesNotSendEmail() {

        Patient patient = new Patient();
        patient.setId(10L);

        RendezVous rdv = new RendezVous();
        rdv.setId(1L);
        rdv.setStatus(RendezVousStatus.CONFIRME);
        rdv.setPatient(patient);

        when(rendezVousRepository.findById(1L)).thenReturn(Optional.of(rdv));

        RendezVousSimpleDto dto = rendezVousService.confirmRendezVous(1L);

        assertEquals("CONFIRME", dto.getStatus());

        verify(rendezVousRepository).findById(1L);
        verify(rendezVousRepository, never()).save(any());
        verify(emailService, never()).sendRdvConfirmationHtml(any(), any(), any(), any());
    }

    @Test
    void confirmRendezVous_whenPatientHasNoEmail_doesNotSendEmail() {
        Patient patient = new Patient();
        patient.setId(10L);
        patient.setNomPatient("Ali");
        patient.setEmail(null); // no email

        Medecin medecin = new Medecin();
        medecin.setNomMedecin("Dr House");

        RendezVous rdv = new RendezVous();
        rdv.setId(1L);
        rdv.setStatus(RendezVousStatus.PLANIFIE);
        rdv.setDateHeure(LocalDateTime.of(2026, 6, 1, 9, 0));
        rdv.setPatient(patient);
        rdv.setMedecin(medecin);

        when(rendezVousRepository.findById(1L)).thenReturn(Optional.of(rdv));
        when(rendezVousRepository.save(any(RendezVous.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RendezVousSimpleDto dto = rendezVousService.confirmRendezVous(1L);

        assertEquals("CONFIRME", dto.getStatus());
        verify(emailService, never()).sendRdvConfirmationHtml(any(), any(), any(), any());
    }

    @Test
    void confirmRendezVous_whenNotFound_throwsException() {
        when(rendezVousRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RendezVousNotFoundException.class,
                () -> rendezVousService.confirmRendezVous(99L));

        verify(rendezVousRepository, never()).save(any());
        verify(emailService, never()).sendRdvConfirmationHtml(any(), any(), any(), any());
    }

    // ─────────────────────────────────────────────────────────────
    // getAllTable
    // ─────────────────────────────────────────────────────────────

    @Test
    void getAllTable_returnsMappedDtos() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setNomPatient("Ali");

        Medecin medecin = new Medecin();
        medecin.setId(2L);
        medecin.setNomMedecin("Dr House");

        RendezVous rdv = new RendezVous();
        rdv.setId(10L);
        rdv.setDateHeure(LocalDateTime.of(2026, 5, 1, 10, 0));
        rdv.setStatus(RendezVousStatus.PLANIFIE);
        rdv.setPatient(patient);
        rdv.setMedecin(medecin);

        when(rendezVousRepository.findAll()).thenReturn(List.of(rdv));

        List<RendezVousTableDto> result = rendezVousService.getAllTable();

        assertEquals(1, result.size());
        RendezVousTableDto dto = result.get(0);
        assertEquals(10L, dto.getId());
        assertEquals("PLANIFIE", dto.getStatus());
        assertEquals("Ali", dto.getPatientNom());
        assertEquals("Dr House", dto.getMedecinNom());
        verify(rendezVousRepository).findAll();
    }

    // ─────────────────────────────────────────────────────────────
    // getRendezVousByMedecinTable
    // ─────────────────────────────────────────────────────────────

    @Test
    void getRendezVousByMedecinTable_returnsMappedDtos() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setNomPatient("Sara");

        Medecin medecin = new Medecin();
        medecin.setId(5L);
        medecin.setNomMedecin("Dr Strange");

        RendezVous rdv = new RendezVous();
        rdv.setId(20L);
        rdv.setDateHeure(LocalDateTime.of(2026, 6, 10, 14, 0));
        rdv.setStatus(RendezVousStatus.CONFIRME);
        rdv.setPatient(patient);
        rdv.setMedecin(medecin);

        when(rendezVousRepository.findByMedecinId(5L)).thenReturn(List.of(rdv));

        List<RendezVousTableDto> result = rendezVousService.getRendezVousByMedecinTable(5L);

        assertEquals(1, result.size());
        assertEquals("CONFIRME", result.get(0).getStatus());
        assertEquals("Sara", result.get(0).getPatientNom());
        verify(rendezVousRepository).findByMedecinId(5L);
    }

    // ─────────────────────────────────────────────────────────────
    // assignCare
    // ─────────────────────────────────────────────────────────────

    @Test
    void assignCare_whenStatusTermine_assignsAndSaves() {
        RendezVous rdv = new RendezVous();
        rdv.setId(1L);
        rdv.setStatus(RendezVousStatus.TERMINE);

        RendezVousCareAssignRequest req = new RendezVousCareAssignRequest(
                Room.A101,
                Nurse.NURSE_AYA,
                List.of(Medication.DONEPE)
        );

        when(rendezVousRepository.findById(1L)).thenReturn(Optional.of(rdv));
        when(rendezVousRepository.save(any(RendezVous.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RendezVous result = rendezVousService.assignCare(1L, req);

        assertEquals(Room.A101, result.getChambre());
        assertEquals(Nurse.NURSE_AYA, result.getInfermiere());
        assertTrue(result.getMedicaments().contains(Medication.DONEPE));
        verify(rendezVousRepository).save(rdv);
    }

    @Test
    void assignCare_whenStatusNotTermine_throwsException() {
        RendezVous rdv = new RendezVous();
        rdv.setId(1L);
        rdv.setStatus(RendezVousStatus.CONFIRME); // not TERMINE

        RendezVousCareAssignRequest req = new RendezVousCareAssignRequest(
                Room.B201,
                Nurse.NURSE_SALMA,
                List.of()
        );

        when(rendezVousRepository.findById(1L)).thenReturn(Optional.of(rdv));

        assertThrows(RuntimeException.class,
                () -> rendezVousService.assignCare(1L, req));

        verify(rendezVousRepository, never()).save(any());
    }

    @Test
    void assignCare_whenNotFound_throwsException() {
        when(rendezVousRepository.findById(99L)).thenReturn(Optional.empty());

        RendezVousCareAssignRequest req = new RendezVousCareAssignRequest(
                Room.A102, Nurse.NURSE_NOUR, List.of()
        );

        assertThrows(RendezVousNotFoundException.class,
                () -> rendezVousService.assignCare(99L, req));

        verify(rendezVousRepository, never()).save(any());
    }
}