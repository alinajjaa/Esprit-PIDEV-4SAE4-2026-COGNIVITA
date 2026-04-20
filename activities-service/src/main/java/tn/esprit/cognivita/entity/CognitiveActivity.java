package tn.esprit.cognivita.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cognitive_activities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CognitiveActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String type; // MEMORY, ATTENTION, LOGIC

    @Column(nullable = false)
    private String difficulty; // EASY, MEDIUM, HARD

    // ===== CHAMPS SPÉCIFIQUES POUR MEMORY =====
    @ElementCollection(fetch = FetchType.EAGER)  // ← IMPORTANT !
    @CollectionTable(name = "activity_words", joinColumns = @JoinColumn(name = "activity_id"))
    @Column(name = "word")
    @OrderColumn(name = "word_order")
    private List<String> words = new ArrayList<>();

    // ===== CHAMPS SPÉCIFIQUES POUR ATTENTION =====
    private String stroopWord;
    private String stroopColor;
    private String stroopCorrect;

    // ===== CHAMPS SPÉCIFIQUES POUR LOGIC =====
    @ElementCollection(fetch = FetchType.EAGER)  // ← IMPORTANT !
    @CollectionTable(name = "activity_sequences", joinColumns = @JoinColumn(name = "activity_id"))
    @Column(name = "sequence_number")
    @OrderColumn(name = "sequence_order")
    private List<Integer> sequence = new ArrayList<>();

    private Integer sequenceAnswer;

    // ===== CHAMPS COMMUNS =====
    private Integer timeLimit;
    private Integer maxScore;
    private String instructions;
    private String imageUrl;

    @Column(nullable = false)
    private Boolean isActive = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL)
    private List<ActivityParticipation> participations = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== MÉTHODES UTILITAIRES =====
    public boolean isMemoryType() {
        return "MEMORY".equals(type);
    }

    public boolean isAttentionType() {
        return "ATTENTION".equals(type);
    }

    public boolean isLogicType() {
        return "LOGIC".equals(type);
    }

    public void addWord(String word) {
        if (words == null) {
            words = new ArrayList<>();
        }
        words.add(word);
    }

    public void addSequenceNumber(Integer number) {
        if (sequence == null) {
            sequence = new ArrayList<>();
        }
        sequence.add(number);
    }
}