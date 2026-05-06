package com.cognivita.dashboardservice.service;

import com.cognivita.dashboardservice.dto.AlertTypePressureDto;
import com.cognivita.dashboardservice.dto.PatientSafetyBurdenDto;
import com.cognivita.dashboardservice.enums.AlertType;
import com.cognivita.dashboardservice.repository.AppointmentAnalyticsRepository;
import com.cognivita.dashboardservice.repository.MmseAnalyticsRepository;
import com.cognivita.dashboardservice.repository.TrackingAlertAnalyticsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TrackingAlertAnalyticsRepository trackingAlertAnalyticsRepository;

    @Mock
    private AppointmentAnalyticsRepository appointmentAnalyticsRepository;

    @Mock
    private MmseAnalyticsRepository mmseAnalyticsRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getAlertPressure_returnsRepositoryAnalyticsData() {
        LocalDateTime from = LocalDateTime.of(2026, 4, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 4, 30, 23, 59);

        List<AlertTypePressureDto> mockedResponse = List.of(
                new AlertTypePressureDto(AlertType.OUT_OF_ZONE, 10L, 3L, 20L, 5.0),
                new AlertTypePressureDto(AlertType.NO_MOVEMENT, 5L, 2L, 5L, 4.0)
        );

        when(trackingAlertAnalyticsRepository.getAlertTypePressure(from, to)).thenReturn(mockedResponse);

        List<AlertTypePressureDto> result = dashboardService.getAlertPressure(from, to);

        verify(trackingAlertAnalyticsRepository, times(1)).getAlertTypePressure(from, to);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(AlertType.OUT_OF_ZONE, result.get(0).getType());
        assertEquals(10L, result.get(0).getTotalCount());
        assertEquals(AlertType.NO_MOVEMENT, result.get(1).getType());
        assertEquals(5L, result.get(1).getTotalCount());
    }

    @Test
    void getPatientSafetyRanking_returnsDataWithOrderingPreserved() {
        LocalDateTime from = LocalDateTime.of(2026, 4, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 4, 30, 23, 59);

        List<PatientSafetyBurdenDto> mockedRanking = List.of(
                new PatientSafetyBurdenDto(1L, 8L, 3L, 3L, 5L, 13L),
                new PatientSafetyBurdenDto(2L, 6L, 2L, 2L, 3L, 8L)
        );

        when(trackingAlertAnalyticsRepository.getPatientSafetyRanking(from, to)).thenReturn(mockedRanking);

        List<PatientSafetyBurdenDto> result = dashboardService.getPatientSafetyRanking(from, to);

        verify(trackingAlertAnalyticsRepository, times(1)).getPatientSafetyRanking(from, to);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getPatientId());
        assertEquals(13L, result.get(0).getRiskScore());
        assertEquals(2L, result.get(1).getPatientId());
        assertEquals(8L, result.get(1).getRiskScore());
        assertTrue(result.get(0).getRiskScore() >= result.get(1).getRiskScore());
    }
}
