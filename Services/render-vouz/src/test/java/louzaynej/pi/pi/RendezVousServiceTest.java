package louzaynej.pi.pi;


import louzaynej.pi.pi.dto.RendezVousSimpleDto;
import louzaynej.pi.pi.exceptions.RendezVousNotFoundException;
import louzaynej.pi.pi.model.Medecin;
import louzaynej.pi.pi.model.Patient;
import louzaynej.pi.pi.model.RendezVous;
import louzaynej.pi.pi.model.RendezVousStatus;
import louzaynej.pi.pi.repositories.RendezVousRepository;
import louzaynej.pi.pi.services.EmailService;
import louzaynej.pi.pi.services.RendezVousService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
}