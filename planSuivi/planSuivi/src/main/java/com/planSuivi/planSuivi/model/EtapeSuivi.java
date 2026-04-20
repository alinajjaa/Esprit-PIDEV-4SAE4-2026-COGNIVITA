package com.planSuivi.planSuivi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EtapeSuivi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    @JsonIgnore
    private PlanSuivi planSuivi;

    private LocalDateTime scheduledDate;

    @Enumerated(EnumType.STRING)
    private StepType type;

    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private StepStatus status;

    private LocalDateTime doneAt;

}
