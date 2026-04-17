package com.cognivita.trackingservice.service;

import com.cognivita.trackingservice.dto.LocationRequestDto;
import com.cognivita.trackingservice.entity.GeofenceZone;
import com.cognivita.trackingservice.entity.PatientLocation;
import com.cognivita.trackingservice.entity.TrackingAlert;
import com.cognivita.trackingservice.enums.AlertType;
import com.cognivita.trackingservice.repository.GeofenceZoneRepository;
import com.cognivita.trackingservice.repository.PatientLocationRepository;
import com.cognivita.trackingservice.repository.TrackingAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TrackingService {

    private static final Logger log = LoggerFactory.getLogger(TrackingService.class);
    private static final long NO_MOVEMENT_THRESHOLD_MINUTES = 1;
    private final PatientLocationRepository patientLocationRepository;
    private final GeofenceZoneRepository geofenceZoneRepository;
    private final TrackingAlertRepository trackingAlertRepository;
    private final EmailService emailService;

    public TrackingService(PatientLocationRepository patientLocationRepository,
                           GeofenceZoneRepository geofenceZoneRepository,
                           TrackingAlertRepository trackingAlertRepository,
                           EmailService emailService) {
        this.patientLocationRepository = patientLocationRepository;
        this.geofenceZoneRepository = geofenceZoneRepository;
        this.trackingAlertRepository = trackingAlertRepository;
        this.emailService = emailService;
    }

    public void handleLocation(LocationRequestDto dto) {
        Long patientId = resolvePatientId(dto.getTid());
        LocalDateTime timestamp = resolveTimestamp(dto.getTst());
        String motionState = extractMotionState(dto);

        PatientLocation location = PatientLocation.builder()
                .patientId(patientId)
                .latitude(dto.getLat())
                .longitude(dto.getLon())
                .timestamp(timestamp)
                .motionState(motionState)
                .build();

        patientLocationRepository.save(location);
        log.info("Location saved for patient {} | lat={} | lon={} | motionState={}",
                patientId, dto.getLat(), dto.getLon(), motionState);
        System.out.println("TRACKING => patientId=" + patientId
                + ", lat=" + dto.getLat()
                + ", lon=" + dto.getLon()
                + ", motionState=" + motionState);

        geofenceZoneRepository.findByPatientId(patientId).ifPresent(zone -> {
            if (dto.getLat() == null || dto.getLon() == null) {
                return;
            }

            double distance = calculateDistance(
                    dto.getLat(),
                    dto.getLon(),
                    zone.getCenterLatitude(),
                    zone.getCenterLongitude()
            );

            if (distance > zone.getRadiusMeters()) {
                boolean unreadExists = trackingAlertRepository
                        .findFirstByPatientIdAndTypeAndReadFalse(patientId, AlertType.OUT_OF_ZONE)
                        .isPresent();

                if (!unreadExists) {
                    TrackingAlert alert = TrackingAlert.builder()
                            .patientId(patientId)
                            .type(AlertType.OUT_OF_ZONE)
                            .message("Patient left safe area")
                            .createdAt(LocalDateTime.now())
                            .read(false)
                            .build();
                    trackingAlertRepository.save(alert);
                    emailService.sendAlertEmail(
                            "sib.benjima@gmail.com",
                            "ALERT: " + alert.getType(),
                            alert.getMessage()
                    );
                }
            }
        });

        if ("stationary".equals(motionState)) {
            evaluateNoMovementForPatient(patientId, LocalDateTime.now(ZoneOffset.UTC), "ingest");
        }
    }

    @Scheduled(fixedDelay = 30000)
    public void scheduledNoMovementCheck() {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        for (PatientLocation latest : getLatestLocationsByPatient()) {
            if ("stationary".equals(latest.getMotionState())) {
                evaluateNoMovementForPatient(latest.getPatientId(), nowUtc, "scheduler");
            }
        }
    }

    private LocalDateTime resolveTimestamp(Long tst) {
        if (tst == null) {
            return LocalDateTime.now(ZoneOffset.UTC);
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(tst), ZoneOffset.UTC);
    }

    private Long resolvePatientId(String tid) {
        if (tid == null || tid.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(tid);
        } catch (NumberFormatException ignored) {
            return Integer.toUnsignedLong(tid.hashCode());
        }
    }

    private String extractMotionState(LocationRequestDto dto) {
        if (dto.getMotionactivities() != null && !dto.getMotionactivities().isEmpty()) {
            String value = dto.getMotionactivities().get(0);
            return value == null ? null : value.trim().toLowerCase();
        }

        if (dto.getVel() != null) {
            if (dto.getVel() > 0) {
                return "moving";
            }
            return "stationary";
        }

        return "unknown";
    }

    public GeofenceZone saveOrUpdateGeofence(Long patientId,
                                             Double centerLatitude,
                                             Double centerLongitude,
                                             Double radiusMeters) {
        GeofenceZone zone = geofenceZoneRepository.findByPatientId(patientId)
                .orElse(GeofenceZone.builder().patientId(patientId).build());

        zone.setCenterLatitude(centerLatitude);
        zone.setCenterLongitude(centerLongitude);
        zone.setRadiusMeters(radiusMeters);

        return geofenceZoneRepository.save(zone);
    }

    public Optional<GeofenceZone> getGeofenceByPatientId(Long patientId) {
        return geofenceZoneRepository.findByPatientId(patientId);
    }

    public void deleteGeofenceByPatientId(Long patientId) {
        geofenceZoneRepository.findByPatientId(patientId)
                .ifPresent(zone -> geofenceZoneRepository.deleteById(zone.getId()));
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public List<PatientLocation> getLatestLocationsByPatient() {
        List<PatientLocation> all = patientLocationRepository.findAllByOrderByTimestampDesc();
        Map<Long, PatientLocation> byPatient = new LinkedHashMap<>();
        for (PatientLocation location : all) {
            byPatient.putIfAbsent(location.getPatientId(), location);
        }
        return List.copyOf(byPatient.values());
    }

    public List<TrackingAlert> getAlerts(Long patientId, boolean unreadOnly) {
        if (patientId != null && unreadOnly) {
            return trackingAlertRepository.findAllByPatientIdAndReadFalseOrderByCreatedAtDesc(patientId);
        }
        if (patientId != null) {
            return trackingAlertRepository.findAllByPatientIdOrderByCreatedAtDesc(patientId);
        }
        if (unreadOnly) {
            return trackingAlertRepository.findAllByReadFalseOrderByCreatedAtDesc();
        }
        return trackingAlertRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<TrackingAlert> markAlertAsRead(Long alertId) {
        Optional<TrackingAlert> opt = trackingAlertRepository.findById(alertId);
        opt.ifPresent(alert -> {
            alert.setRead(true);
            trackingAlertRepository.save(alert);
        });
        return opt;
    }

    private void evaluateNoMovementForPatient(Long patientId, LocalDateTime nowUtc, String source) {
        var lastMovingOpt = patientLocationRepository
                .findFirstByPatientIdAndMotionStateNotOrderByTimestampDesc(patientId, "stationary");

        if (lastMovingOpt.isPresent()) {
            processInactivityTimer(patientId, lastMovingOpt.get().getTimestamp(), nowUtc, source, "moving_baseline");
            return;
        }

        var firstStationaryOpt = patientLocationRepository
                .findFirstByPatientIdAndMotionStateOrderByTimestampAsc(patientId, "stationary");

        if (firstStationaryOpt.isPresent()) {
            processInactivityTimer(patientId, firstStationaryOpt.get().getTimestamp(), nowUtc, source, "stationary_baseline");
            return;
        }

        log.info("NO_MOVEMENT timer [{}] | patient={} | waiting for stationary baseline", source, patientId);
        System.out.println("NO_MOVEMENT timer => patientId=" + patientId
                + ", status=waiting_for_stationary_baseline"
                + ", source=" + source);
    }

    private void processInactivityTimer(Long patientId,
                                        LocalDateTime baselineTime,
                                        LocalDateTime nowUtc,
                                        String source,
                                        String mode) {
        Duration inactivity = Duration.between(baselineTime, nowUtc);
        long minutes = inactivity.toMinutes();
        long thresholdSeconds = NO_MOVEMENT_THRESHOLD_MINUTES * 60;
        long remainingSeconds = Math.max(0, thresholdSeconds - inactivity.getSeconds());
        long remainingMinPart = remainingSeconds / 60;
        long remainingSecPart = remainingSeconds % 60;

        log.info("NO_MOVEMENT timer [{}] | patient={} | inactive={}m | alert in {}m {}s | mode={}",
                source, patientId, minutes, remainingMinPart, remainingSecPart, mode);
        System.out.println("NO_MOVEMENT timer => patientId=" + patientId
                + ", inactiveMinutes=" + minutes
                + ", alertIn=" + remainingMinPart + "m " + remainingSecPart + "s"
                + ", mode=" + mode
                + ", source=" + source);

        if (minutes >= NO_MOVEMENT_THRESHOLD_MINUTES) {
            boolean unreadExists = trackingAlertRepository
                    .findFirstByPatientIdAndTypeAndReadFalse(patientId, AlertType.NO_MOVEMENT)
                    .isPresent();

            if (!unreadExists) {
                TrackingAlert alert = TrackingAlert.builder()
                        .patientId(patientId)
                        .type(AlertType.NO_MOVEMENT)
                        .message("Patient has been inactive for over " + NO_MOVEMENT_THRESHOLD_MINUTES + " minutes")
                        .createdAt(LocalDateTime.now())
                        .read(false)
                        .build();
                trackingAlertRepository.save(alert);
                emailService.sendAlertEmail(
                        "sib.benjima@gmail.com",
                        "ALERT: " + alert.getType(),
                        alert.getMessage()
                );
            }
        }
    }
}
