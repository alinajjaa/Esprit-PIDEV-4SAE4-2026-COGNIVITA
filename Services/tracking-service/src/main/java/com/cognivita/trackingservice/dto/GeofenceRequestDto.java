package com.cognivita.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeofenceRequestDto {
    private Long patientId;
    private Double centerLatitude;
    private Double centerLongitude;
    private Double radiusMeters;
}
