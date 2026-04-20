package louzaynej.pi.pi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(indexes = {
        @Index(name = "idx_rdv_medecin_date", columnList = "medecin_id,dateHeure"),
        @Index(name = "idx_rdv_patient_date", columnList = "patient_id,dateHeure")
})
public class RendezVous {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime dateHeure;

    @Enumerated(EnumType.STRING)
    private RendezVousStatus status = RendezVousStatus.PLANIFIE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medecin_id")
    private Medecin medecin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Room chambre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Nurse infermiere;

    @ElementCollection(targetClass = Medication.class)
    @CollectionTable(
            name = "rendezvous_medicaments",
            joinColumns = @JoinColumn(name = "rendezvous_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "medicament")
    private List<Medication> medicaments = new ArrayList<>();
}
