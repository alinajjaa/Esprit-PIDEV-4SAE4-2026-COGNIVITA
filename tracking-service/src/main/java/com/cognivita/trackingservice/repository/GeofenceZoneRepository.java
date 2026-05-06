package com.cognivita.trackingservice.repository;

import com.cognivita.trackingservice.entity.GeofenceZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GeofenceZoneRepository extends JpaRepository<GeofenceZone, Long> {
    Optional<GeofenceZone> findByPatientId(Long patientId);
}
