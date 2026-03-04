package com.planSuivi.planSuivi.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
public class RendezVousSimpleDto {
    private Long id;
    private LocalDateTime dateHeure;
    private String status;
    private Long patientId;
}