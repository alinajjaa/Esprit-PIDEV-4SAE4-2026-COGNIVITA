package louzaynej.pi.pi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class RendezVousTableDto {

    private Long id;
    private LocalDateTime dateHeure;
    private String status;

    private Long patientId;
    private String patientNom;

    private Long medecinId;
    private String medecinNom;

    private String chambre;
    private String infermiere;

    private List<String> medicaments;
}