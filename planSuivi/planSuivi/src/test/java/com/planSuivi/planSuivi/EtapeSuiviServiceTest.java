package com.planSuivi.planSuivi;

import com.planSuivi.planSuivi.model.EtapeSuivi;
import com.planSuivi.planSuivi.model.StepStatus;
import com.planSuivi.planSuivi.repositories.EtapeSuiviRepository;
import com.planSuivi.planSuivi.services.EtapeSuiviService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EtapeSuiviServiceTest {

    @Mock
    private EtapeSuiviRepository etapeRepo;

    private EtapeSuiviService etapeSuiviService;

    @BeforeEach
    void setup() {
        etapeSuiviService = new EtapeSuiviService(etapeRepo);
    }

    @Test
    void markDone_whenNotFound_throwsNotFound() {
        when(etapeRepo.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> etapeSuiviService.markDone(1L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(etapeRepo).findById(1L);
        verify(etapeRepo, never()).save(any());
    }

    @Test
    void markDone_whenAlreadyDone_returnsStepWithoutSaving() {
        EtapeSuivi step = new EtapeSuivi();
        step.setId(2L);
        step.setStatus(StepStatus.DONE);

        when(etapeRepo.findById(2L)).thenReturn(Optional.of(step));

        EtapeSuivi result = etapeSuiviService.markDone(2L);

        assertSame(step, result);
        assertEquals(StepStatus.DONE, result.getStatus());

        verify(etapeRepo).findById(2L);
        verify(etapeRepo, never()).save(any());
    }

    @Test
    void markDone_whenPending_setsDoneAndSaves() {
        EtapeSuivi step = new EtapeSuivi();
        step.setId(3L);
        step.setStatus(StepStatus.PENDING);

        when(etapeRepo.findById(3L)).thenReturn(Optional.of(step));
        when(etapeRepo.save(any(EtapeSuivi.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EtapeSuivi result = etapeSuiviService.markDone(3L);

        assertEquals(StepStatus.DONE, result.getStatus());
        assertNotNull(result.getDoneAt());
        verify(etapeRepo).findById(3L);
        verify(etapeRepo).save(step);
    }
}