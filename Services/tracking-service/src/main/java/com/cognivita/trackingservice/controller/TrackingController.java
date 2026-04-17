package com.cognivita.trackingservice.controller;

import com.cognivita.trackingservice.dto.GeofenceRequestDto;
import com.cognivita.trackingservice.dto.LocationRequestDto;
import com.cognivita.trackingservice.entity.GeofenceZone;
import com.cognivita.trackingservice.entity.PatientLocation;
import com.cognivita.trackingservice.entity.TrackingAlert;
import com.cognivita.trackingservice.service.TrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/track")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @PostMapping("/location")
    public ResponseEntity<Map<String, String>> ingestLocation(@RequestBody LocationRequestDto dto) {
        trackingService.handleLocation(dto);
        return ResponseEntity.ok(Map.of("message", "Location stored"));
    }

    @PostMapping("/geofence")
    public ResponseEntity<Map<String, String>> upsertGeofence(@RequestBody GeofenceRequestDto dto) {
        trackingService.saveOrUpdateGeofence(
                dto.getPatientId(),
                dto.getCenterLatitude(),
                dto.getCenterLongitude(),
                dto.getRadiusMeters()
        );
        return ResponseEntity.ok(Map.of("message", "Geofence stored"));
    }

    @GetMapping("/location/latest")
    public ResponseEntity<List<PatientLocation>> getLatestLocations() {
        return ResponseEntity.ok(trackingService.getLatestLocationsByPatient());
    }

    @GetMapping("/geofence/{patientId}")
    public ResponseEntity<GeofenceZone> getGeofence(@PathVariable Long patientId) {
        return trackingService.getGeofenceByPatientId(patientId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/geofence/{patientId}")
    public ResponseEntity<Map<String, String>> deleteGeofence(@PathVariable Long patientId) {
        trackingService.deleteGeofenceByPatientId(patientId);
        return ResponseEntity.ok(Map.of("message", "Geofence deleted"));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<TrackingAlert>> getAlerts(
            @RequestParam(required = false) Long patientId,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        return ResponseEntity.ok(trackingService.getAlerts(patientId, unreadOnly));
    }

    @PatchMapping("/alerts/{alertId}/read")
    public ResponseEntity<Map<String, String>> markAlertAsRead(@PathVariable Long alertId) {
        return trackingService.markAlertAsRead(alertId)
                .map(alert -> ResponseEntity.ok(Map.of("message", "Alert marked as read")))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
