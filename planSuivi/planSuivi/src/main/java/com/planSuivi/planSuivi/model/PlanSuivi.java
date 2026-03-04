package com.planSuivi.planSuivi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlanSuivi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long rdvId;

    private Long patientId;

    private LocalDateTime rdvDate;
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private PlanStatus status;

    private String notes;

    @OneToMany(mappedBy = "planSuivi", cascade = CascadeType.ALL, orphanRemoval = true,fetch = FetchType.EAGER)
    private List<EtapeSuivi> etapes;

}
