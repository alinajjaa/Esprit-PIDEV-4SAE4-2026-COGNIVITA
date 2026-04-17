package com.cognivita.trackingservice.repository;

import com.cognivita.trackingservice.entity.TrackingAlert;
import com.cognivita.trackingservice.enums.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrackingAlertRepository extends JpaRepository<TrackingAlert, Long> {
    Optional<TrackingAlert> findFirstByPatientIdAndTypeAndReadFalse(Long patientId, AlertType type);
    List<TrackingAlert> findAllByOrderByCreatedAtDesc();
    List<TrackingAlert> findAllByReadFalseOrderByCreatedAtDesc();
    List<TrackingAlert> findAllByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<TrackingAlert> findAllByPatientIdAndReadFalseOrderByCreatedAtDesc(Long patientId);
}
