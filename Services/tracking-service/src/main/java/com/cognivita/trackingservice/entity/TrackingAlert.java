package com.cognivita.trackingservice.entity;

import com.cognivita.trackingservice.enums.AlertType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tracking_alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patientId;

    @Enumerated(EnumType.STRING)
    private AlertType type;

    private String message;

    private LocalDateTime createdAt;

    @Column(name = "is_read")
    private Boolean read;
}
