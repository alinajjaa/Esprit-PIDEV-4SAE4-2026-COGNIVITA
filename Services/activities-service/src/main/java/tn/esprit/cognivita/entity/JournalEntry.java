package tn.esprit.cognivita.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journal_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotNull
    private LocalDate date;

    @Column(nullable = false)
    @NotNull
    @Min(1)
    @Max(5)
    private Integer mood;

    @Column(nullable = false)
    @NotNull
    @Min(1)
    @Max(5)
    private Integer energy;

    @Column(nullable = false)
    @NotNull
    @Min(1)
    @Max(5)
    private Integer stress;

    @Column(nullable = false)
    @NotNull
    private Double sleepHours;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "journal_activities", joinColumns = @JoinColumn(name = "entry_id"))
    @Column(name = "activity")
    private List<String> activities = new ArrayList<>();

    @Column(length = 1000)
    @Size(max = 1000)
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

