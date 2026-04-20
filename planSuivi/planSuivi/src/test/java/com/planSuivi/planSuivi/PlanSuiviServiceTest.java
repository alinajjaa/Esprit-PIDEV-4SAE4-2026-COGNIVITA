package com.planSuivi.planSuivi;

import com.planSuivi.planSuivi.client.RendezVousClient;
import com.planSuivi.planSuivi.dto.RendezVousSimpleDto;
import com.planSuivi.planSuivi.model.PlanSuivi;
import com.planSuivi.planSuivi.model.PlanStatus;
import com.planSuivi.planSuivi.repositories.PlanSuiviRepository;
import com.planSuivi.planSuivi.services.PlanSuiviService;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanSuiviServiceTest {

    @Mock
    private PlanSuiviRepository planRepo;

    @Mock
    private RendezVousClient rdvClient;

    private PlanSuiviService planSuiviService;

    @BeforeEach
    void setup() {
        planSuiviService = new PlanSuiviService(planRepo, rdvClient);
    }

    @Test
    void createPlanFromRdv_whenDuplicate_throwsConflict() {
        when(planRepo.findByRdvId(5L)).thenReturn(Optional.of(new PlanSuivi()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> planSuiviService.createPlanFromRdv(5L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(planRepo).findByRdvId(5L);
        verifyNoInteractions(rdvClient);
        verify(planRepo, never()).save(any());
    }

    @Test
    void createPlanFromRdv_whenRdvNotFound_throwsNotFound() {
        when(planRepo.findByRdvId(10L)).thenReturn(Optional.empty());

        // Create a Feign NotFound exception
        Request req = Request.create(Request.HttpMethod.GET, "/api/rendezvous/10/simple",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);

        Response resp = Response.builder()
                .request(req)
                .status(404)
                .reason("Not Found")
                .headers(Collections.emptyMap())
                .build();

        when(rdvClient.getSimpleById(10L)).thenThrow(FeignException.errorStatus("getSimpleById", resp));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> planSuiviService.createPlanFromRdv(10L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(planRepo).findByRdvId(10L);
        verify(rdvClient).getSimpleById(10L);
        verify(planRepo, never()).save(any());
    }

    @Test
    void createPlanFromRdv_whenFeignGenericError_throwsBadGateway() {
        when(planRepo.findByRdvId(20L)).thenReturn(Optional.empty());

        // Create a Feign generic error (503)
        Request req = Request.create(Request.HttpMethod.GET, "/api/rendezvous/20/simple",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);

        Response resp = Response.builder()
                .request(req)
                .status(503)
                .reason("Service Unavailable")
                .headers(Collections.emptyMap())
                .build();

        when(rdvClient.getSimpleById(20L)).thenThrow(FeignException.errorStatus("getSimpleById", resp));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> planSuiviService.createPlanFromRdv(20L));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
        verify(planRepo).findByRdvId(20L);
        verify(rdvClient).getSimpleById(20L);
        verify(planRepo, never()).save(any());
    }

    @Test
    void createPlanFromRdv_whenPatientIdNull_throwsBadRequest() {
        when(planRepo.findByRdvId(30L)).thenReturn(Optional.empty());

        RendezVousSimpleDto rdv = new RendezVousSimpleDto();
        rdv.setId(30L);
        rdv.setPatientId(null); // patientId manquant
        rdv.setDateHeure(LocalDateTime.of(2026, 3, 1, 9, 0));
        rdv.setStatus("CONFIRME");

        when(rdvClient.getSimpleById(30L)).thenReturn(rdv);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> planSuiviService.createPlanFromRdv(30L));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(planRepo).findByRdvId(30L);
        verify(rdvClient).getSimpleById(30L);
        verify(planRepo, never()).save(any());
    }

    @Test
    void createPlanFromRdv_whenOk_savesPlanWith3Steps() {
        when(planRepo.findByRdvId(1L)).thenReturn(Optional.empty());

        RendezVousSimpleDto rdv = new RendezVousSimpleDto();
        rdv.setId(1L);
        rdv.setPatientId(99L);
        rdv.setDateHeure(LocalDateTime.of(2026, 2, 25, 10, 0));
        rdv.setStatus("CONFIRME");

        when(rdvClient.getSimpleById(1L)).thenReturn(rdv);

        when(planRepo.save(any(PlanSuivi.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PlanSuivi saved = planSuiviService.createPlanFromRdv(1L);

        assertNotNull(saved);
        assertEquals(1L, saved.getRdvId());
        assertEquals(99L, saved.getPatientId());
        assertEquals(PlanStatus.ACTIVE, saved.getStatus());
        assertNotNull(saved.getEtapes());
        assertEquals(3, saved.getEtapes().size());

        // optional: verify dates are computed from rdv date
        assertEquals(rdv.getDateHeure().plusDays(7), saved.getEtapes().get(0).getScheduledDate());
        assertEquals(rdv.getDateHeure().plusDays(30), saved.getEtapes().get(1).getScheduledDate());
        assertEquals(rdv.getDateHeure().plusDays(90), saved.getEtapes().get(2).getScheduledDate());

        verify(planRepo).findByRdvId(1L);
        verify(rdvClient).getSimpleById(1L);
        verify(planRepo).save(any(PlanSuivi.class));
    }

    @Test
    void getPlansByPatient_whenPlansExist_returnsList() {
        PlanSuivi plan1 = new PlanSuivi();
        plan1.setId(1L);
        plan1.setPatientId(42L);
        plan1.setStatus(PlanStatus.ACTIVE);

        PlanSuivi plan2 = new PlanSuivi();
        plan2.setId(2L);
        plan2.setPatientId(42L);
        plan2.setStatus(PlanStatus.COMPLETED);

        when(planRepo.findAllByPatientId(42L)).thenReturn(List.of(plan1, plan2));

        List<PlanSuivi> result = planSuiviService.getPlansByPatient(42L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(42L, result.get(0).getPatientId());
        assertEquals(42L, result.get(1).getPatientId());

        verify(planRepo).findAllByPatientId(42L);
    }

    @Test
    void getPlansByPatient_whenNoPlans_returnsEmptyList() {
        when(planRepo.findAllByPatientId(99L)).thenReturn(Collections.emptyList());

        List<PlanSuivi> result = planSuiviService.getPlansByPatient(99L);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(planRepo).findAllByPatientId(99L);
    }
}