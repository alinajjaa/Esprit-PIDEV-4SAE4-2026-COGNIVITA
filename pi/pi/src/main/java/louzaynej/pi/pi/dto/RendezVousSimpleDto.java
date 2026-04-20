package louzaynej.pi.pi.dto;

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

    public RendezVousSimpleDto(Long id, LocalDateTime dateHeure, String status, Long patientId) {
        this.id = id;
        this.dateHeure = dateHeure;
        this.status = status;
        this.patientId = patientId;
    }

}