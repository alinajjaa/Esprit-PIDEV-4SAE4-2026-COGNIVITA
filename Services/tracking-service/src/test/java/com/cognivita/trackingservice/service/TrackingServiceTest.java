package com.cognivita.trackingservice.service;

import com.cognivita.trackingservice.dto.LocationRequestDto;
import com.cognivita.trackingservice.entity.GeofenceZone;
import com.cognivita.trackingservice.entity.PatientLocation;
import com.cognivita.trackingservice.entity.TrackingAlert;
import com.cognivita.trackingservice.enums.AlertType;
import com.cognivita.trackingservice.repository.GeofenceZoneRepository;
import com.cognivita.trackingservice.repository.PatientLocationRepository;
import com.cognivita.trackingservice.repository.TrackingAlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock
    private PatientLocationRepository patientLocationRepository;

    @Mock
    private GeofenceZoneRepository geofenceZoneRepository;

    @Mock
    private TrackingAlertRepository trackingAlertRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private TrackingService trackingService;

    @Test
    void serviceIsCreatedWithMocks() {
        assertNotNull(trackingService);
    }

    @Test
    void handleLocation_createsOutOfZoneAlert_whenPatientIsOutsideGeofenceAndNoUnreadAlertExists() {
        Long patientId = 1L;

        LocationRequestDto dto = LocationRequestDto.builder()
                .tid(String.valueOf(patientId))
                .lat(36.9000)
                .lon(10.3000)
                .vel(1.0)
                .build();

        GeofenceZone zone = GeofenceZone.builder()
                .patientId(patientId)
                .centerLatitude(36.8000)
                .centerLongitude(10.1000)
                .radiusMeters(100.0)
                .build();

        when(geofenceZoneRepository.findByPatientId(patientId)).thenReturn(Optional.of(zone));
        when(trackingAlertRepository.findFirstByPatientIdAndTypeAndReadFalse(patientId, AlertType.OUT_OF_ZONE))
                .thenReturn(Optional.empty());

        trackingService.handleLocation(dto);

        ArgumentCaptor<TrackingAlert> alertCaptor = ArgumentCaptor.forClass(TrackingAlert.class);
        verify(trackingAlertRepository, times(1)).save(alertCaptor.capture());

        TrackingAlert savedAlert = alertCaptor.getValue();
        assertNotNull(savedAlert);
        assertEquals(patientId, savedAlert.getPatientId());
        assertEquals(AlertType.OUT_OF_ZONE, savedAlert.getType());
        assertEquals("Patient left safe area", savedAlert.getMessage());
        assertFalse(savedAlert.getRead());

        verify(trackingAlertRepository, times(1))
                .findFirstByPatientIdAndTypeAndReadFalse(patientId, AlertType.OUT_OF_ZONE);
        verify(patientLocationRepository, times(1)).save(any());
        verify(geofenceZoneRepository, times(1)).findByPatientId(eq(patientId));
        verify(emailService, times(1)).sendAlertEmail(
                "sib.benjima@gmail.com",
                "ALERT: " + AlertType.OUT_OF_ZONE,
                "Patient left safe area"
        );
    }

    @Test
    void handleLocation_createsNoMovementAlert_whenPatientStaysStationaryPastThreshold() {
        Long patientId = 1L;
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

        LocationRequestDto dto = LocationRequestDto.builder()
                .tid(String.valueOf(patientId))
                .lat(36.8000)
                .lon(10.1000)
                .tst(nowUtc.toEpochSecond(ZoneOffset.UTC))
                .motionactivities(List.of("stationary"))
                .build();

        PatientLocation lastMovingLocation = PatientLocation.builder()
                .patientId(patientId)
                .timestamp(nowUtc.minusMinutes(15))
                .motionState("moving")
                .build();

        when(geofenceZoneRepository.findByPatientId(patientId)).thenReturn(Optional.empty());
        when(patientLocationRepository.findFirstByPatientIdAndMotionStateNotOrderByTimestampDesc(patientId, "stationary"))
                .thenReturn(Optional.of(lastMovingLocation));
        when(trackingAlertRepository.findFirstByPatientIdAndTypeAndReadFalse(patientId, AlertType.NO_MOVEMENT))
                .thenReturn(Optional.empty());

        trackingService.handleLocation(dto);

        ArgumentCaptor<TrackingAlert> alertCaptor = ArgumentCaptor.forClass(TrackingAlert.class);
        verify(trackingAlertRepository, times(1)).save(alertCaptor.capture());

        TrackingAlert savedAlert = alertCaptor.getValue();
        assertNotNull(savedAlert);
        assertEquals(patientId, savedAlert.getPatientId());
        assertEquals(AlertType.NO_MOVEMENT, savedAlert.getType());
        assertTrue(savedAlert.getMessage().contains("inactive"));
        assertFalse(savedAlert.getRead());

        verify(patientLocationRepository, times(1))
                .findFirstByPatientIdAndMotionStateNotOrderByTimestampDesc(patientId, "stationary");
        verify(trackingAlertRepository, times(1))
                .findFirstByPatientIdAndTypeAndReadFalse(patientId, AlertType.NO_MOVEMENT);
        verify(emailService, times(1)).sendAlertEmail(
                "sib.benjima@gmail.com",
                "ALERT: " + AlertType.NO_MOVEMENT,
                savedAlert.getMessage()
        );
    }
}
