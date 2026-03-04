package louzaynej.pi.pi.dto;

import java.time.LocalDateTime;

public record RendezVousCalendarDto(
        Long id,
        LocalDateTime dateHeure,
        String status,
        String patientName,
        String chambre
) {}